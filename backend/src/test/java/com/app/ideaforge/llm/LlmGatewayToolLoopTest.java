package com.app.ideaforge.llm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.app.ideaforge.common.config.AppProperties;
import com.app.ideaforge.security.SecurityEventService;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** The gateway's manual tool loop: execute, cap, and feed results back. */
class LlmGatewayToolLoopTest {

	private AiUsageLogger usageLogger;
	private SecurityEventService securityEvents;
	private AppProperties properties;

	@BeforeEach
	void setUp() {
		usageLogger = mock(AiUsageLogger.class);
		securityEvents = mock(SecurityEventService.class);
		properties = properties();
	}

	@Test
	void executesToolsThenReturnsText() {
		AtomicBoolean executed = new AtomicBoolean(false);
		List<ChatRequest> requests = new ArrayList<>();
		ChatModel model = model(request -> {
			requests.add(request);
			if (requests.size() == 1) {
				return ChatResponse.builder()
						.aiMessage(new AiMessage("", List.of(ToolExecutionRequest.builder()
								.id("call-1")
								.name("web_search")
								.arguments("{\"query\":\"dental\"}")
								.build())))
						.tokenUsage(new TokenUsage(10, 5))
						.build();
			}
			return ChatResponse.builder()
					.aiMessage(new AiMessage("Found two competitors."))
					.tokenUsage(new TokenUsage(20, 10))
					.build();
		});
		LlmTool tool = new LlmTool() {
			@Override
			public String name() {
				return "web_search";
			}

			@Override
			public String description() {
				return "test tool";
			}

			@Override
			public List<ToolParameter> parameters() {
				return List.of(new ToolParameter("query", "string", "query", true));
			}

			@Override
			public String execute(String jsonArguments) {
				executed.set(true);
				assertThat(jsonArguments).contains("dental");
				return "[{\"title\":\"Competitor\"}]";
			}
		};

		LlmToolChatResult result = gatewayWith(model).chatWithTools(
				LlmPurpose.CHAT, new LlmPrompt("system", "user"), List.of(tool), 3);

		assertThat(executed).isTrue();
		assertThat(result.text()).isEqualTo("Found two competitors.");
		assertThat(result.toolInvocations()).containsExactly("web_search");
		assertThat(result.usage().inputTokens()).isEqualTo(30);
		assertThat(requests).hasSize(2);
		assertThat(requests.get(1).messages())
				.anyMatch(message -> message instanceof ToolExecutionResultMessage);
	}

	@Test
	void blocksFurtherToolCallsAtTheLimit() {
		List<ChatRequest> requests = new ArrayList<>();
		ChatModel model = model(request -> {
			requests.add(request);
			if (requests.size() <= 3) {
				return ChatResponse.builder()
						.aiMessage(new AiMessage("", List.of(ToolExecutionRequest.builder()
								.id("call-" + requests.size())
								.name("web_search")
								.arguments("{}")
								.build())))
						.build();
			}
			return ChatResponse.builder().aiMessage(new AiMessage("done")).build();
		});
		LlmTool tool = new LlmTool() {
			@Override
			public String name() {
				return "web_search";
			}

			@Override
			public String description() {
				return "test tool";
			}

			@Override
			public List<ToolParameter> parameters() {
				return List.of();
			}

			@Override
			public String execute(String jsonArguments) {
				return "ok";
			}
		};

		LlmToolChatResult result = gatewayWith(model).chatWithTools(
				LlmPurpose.CHAT, new LlmPrompt("s", "u"), List.of(tool), 2);

		assertThat(result.toolInvocations()).hasSize(2);
		assertThat(result.text()).isEqualTo("done");
	}

	private interface Responder {
		ChatResponse respond(ChatRequest request);
	}

	private ChatModel model(Responder responder) {
		return new ChatModel() {
			@Override
			public ChatResponse chat(ChatRequest request) {
				return responder.respond(request);
			}
		};
	}

	private LlmGateway gatewayWith(ChatModel model) {
		LlmModelRegistry registry = new LlmModelRegistry(properties) {
			@Override
			public ChatModel modelFor(String profileName) {
				return model;
			}
		};
		return new LangChain4jLlmGateway(
				registry, properties, usageLogger, mock(LlmBudgetGuard.class), securityEvents,
				new tools.jackson.databind.ObjectMapper());
	}

	private AppProperties properties() {
		AppProperties.Llm.LlmProfile chat = new AppProperties.Llm.LlmProfile(
				"ollama", "fake-model", 0.5, 1000, 30, null, null);
		return new AppProperties(
				null, null, null, null, null, null, null, null,
				new AppProperties.Llm(
						BigDecimal.ZERO,
						Map.of("chat", chat),
						Map.of("ollama", new AppProperties.Llm.ProviderConfig(null, "http://localhost:11434")),
						new AppProperties.Llm.Pricing(BigDecimal.ZERO, BigDecimal.ZERO, Map.of(), Map.of())),
				null,
				null);
	}
}
