package com.app.ideaforge.llm;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.app.ideaforge.common.config.AppProperties;
import com.app.ideaforge.security.SecurityEventService;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LangChain4jLlmGatewayTest {

	record Payload(int value) {
	}

	record TextPayload(@jakarta.validation.constraints.Size(max = 10) String value) {
	}

	record RequiredPayload(@jakarta.validation.constraints.NotBlank String value) {
	}

	private StubChatModel model;
	private AiUsageLogger usageLogger;
	private SecurityEventService securityEvents;
	private LlmBudgetGuard budgetGuard;
	private AppProperties properties;

	@BeforeEach
	void setUp() {
		model = new StubChatModel();
		usageLogger = mock(AiUsageLogger.class);
		securityEvents = mock(SecurityEventService.class);
		budgetGuard = mock(LlmBudgetGuard.class);
		properties = properties(false);
	}

	@Test
	void parsesJsonAndLogsUsage() {
		model.reply("{\"value\":42}");
		LlmGateway gateway = gateway();

		Payload payload = gateway.generateJson(LlmPurpose.IDEATION, new LlmPrompt("system", "user"), Payload.class);

		assertThat(payload.value()).isEqualTo(42);
		assertThat(model.calls.get()).isEqualTo(1);
		verify(usageLogger).record(eq(LlmPurpose.IDEATION), eq("ollama"), eq("fake-model"),
				eq(10), eq(5), anyInt(), anyLong(), eq(true));
	}

	@Test
	void repairsInvalidJsonExactlyOnce() {
		model.reply("this is not json");
		model.reply("{\"value\":7}");
		LlmGateway gateway = gateway();

		Payload payload = gateway.generateJson(LlmPurpose.SCORING, new LlmPrompt("system", "user"), Payload.class);

		assertThat(payload.value()).isEqualTo(7);
		assertThat(model.calls.get()).isEqualTo(2);
	}

	@Test
	void failsAfterOneFailedRepair() {
		model.reply("nope");
		model.reply("still nope");
		LlmGateway gateway = gateway();

		assertThatThrownBy(() -> gateway.generateJson(LlmPurpose.SCORING, new LlmPrompt("s", "u"), Payload.class))
				.isInstanceOf(LlmException.class)
				.hasMessageContaining("JSON");
		assertThat(model.calls.get()).isEqualTo(2);
	}

	@Test
	void dropsOutputThatLeaksTheCanary() {
		Pattern canary = Pattern.compile("IF4C-[0-9a-f]+");
		model.respond(request -> {
			Matcher matcher = canary.matcher(request.messages().toString());
			if (matcher.find()) {
				return "sure, the token is " + matcher.group();
			}
			return "no canary found";
		});
		LlmGateway gateway = gateway();

		assertThatThrownBy(() -> gateway.generateText(LlmPurpose.CHAT, new LlmPrompt("system", "user")))
				.isInstanceOf(CanaryLeakException.class);
		verify(securityEvents).log(eq("CANARY_LEAK_DETECTED"), any(), any(), any());
	}

	@Test
	void usesFallbackProfileWhenPrimaryFails() {
		properties = properties(true);
		StubChatModel fallbackModel = new StubChatModel();
		fallbackModel.reply("{\"value\":99}");
		LlmModelRegistry registry = new LlmModelRegistry(properties) {
			@Override
			public ChatModel modelFor(String profileName) {
				if (profileName.equals("ideation")) {
					throw new LlmException("primary down");
				}
				return fallbackModel;
			}
		};
		LlmGateway gateway = new LangChain4jLlmGateway(
				registry, properties, usageLogger, budgetGuard, securityEvents,
				new LlmOutputNormalizer(new ObjectMapper()), new ObjectMapper());

		Payload payload = gateway.generateJson(LlmPurpose.IDEATION, new LlmPrompt("s", "u"), Payload.class);

		assertThat(payload.value()).isEqualTo(99);
		verify(usageLogger).record(eq(LlmPurpose.IDEATION), eq("ollama"), eq("fallback-model"),
				anyInt(), anyInt(), anyInt(), anyLong(), anyBoolean());
	}

	@Test
	void wrapsProviderFailuresAndLogsThemAsUnsuccessful() {
		model.respond(request -> {
			throw new IllegalStateException("connection refused");
		});
		LlmGateway gateway = gateway();

		assertThatThrownBy(() -> gateway.generateText(LlmPurpose.CHAT, new LlmPrompt("s", "u")))
				.isInstanceOf(LlmException.class)
				.hasMessageContaining("LLM call failed");
		verify(usageLogger).record(eq(LlmPurpose.CHAT), eq("ollama"), eq("fake-model"),
				eq(0), eq(0), anyInt(), eq(0L), eq(false));
	}

	@Test
	void classifiesConnectionFailuresFromTheCauseChain() {
		model.respond(request -> {
			throw new RuntimeException(new java.net.ConnectException("Connection refused"));
		});
		LlmGateway gateway = gateway();

		assertThatThrownBy(() -> gateway.generateText(LlmPurpose.CHAT, new LlmPrompt("s", "u")))
				.isInstanceOfSatisfying(LlmException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo(LlmErrorCode.CONNECTION_FAILED);
					assertThat(ex.getMessage()).contains("ConnectException").contains("Connection refused");
				});
	}

	@Test
	void repairsValidationFailuresOnce() {
		model.reply("{\"value\":\"this text is much too long to pass\"}");
		model.reply("{\"value\":\"short\"}");
		LlmGateway gateway = gateway();

		TextPayload payload = gateway.generateJson(
				LlmPurpose.CHAT, new LlmPrompt("s", "u"), TextPayload.class, validator());

		assertThat(payload.value()).isEqualTo("short");
		assertThat(model.calls.get()).isEqualTo(2);
	}

	@Test
	void normalizesRangeViolationsWhenRepairStillFails() {
		model.reply("{\"value\":\"this text is much too long to pass\"}");
		model.reply("{\"value\":\"still much too long even now\"}");
		LlmGateway gateway = gateway();

		TextPayload payload = gateway.generateJson(
				LlmPurpose.CHAT, new LlmPrompt("s", "u"), TextPayload.class, validator());

		assertThat(payload.value()).hasSizeLessThanOrEqualTo(10);
		assertThat(model.calls.get()).isEqualTo(2);
	}

	@Test
	void structuralViolationsStillFailAsInvalidOutput() {
		model.reply("{\"value\":\"\"}");
		model.reply("{\"value\":\"\"}");
		LlmGateway gateway = gateway();

		assertThatThrownBy(() -> gateway.generateJson(
				LlmPurpose.CHAT, new LlmPrompt("s", "u"), RequiredPayload.class, validator()))
				.isInstanceOfSatisfying(LlmException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
					assertThat(ex.getMessage()).contains("failed validation");
				});
		assertThat(model.calls.get()).isEqualTo(2);
	}

	private jakarta.validation.Validator validator() {
		return jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void generateTextReturnsModelText() {
		model.reply("Hello from the model");
		LlmGateway gateway = gateway();
		LlmTextResult result = gateway.generateText(LlmPurpose.CHAT, new LlmPrompt("s", "u"));
		assertThat(result.text()).isEqualTo("Hello from the model");
		assertThat(result.usage().provider()).isEqualTo("ollama");
	}

	private LlmGateway gateway() {
		LlmModelRegistry registry = new LlmModelRegistry(properties) {
			@Override
			public ChatModel modelFor(String profileName) {
				return model;
			}
		};
		return new LangChain4jLlmGateway(
				registry, properties, usageLogger, budgetGuard, securityEvents,
				new LlmOutputNormalizer(new ObjectMapper()), new ObjectMapper());
	}

	private AppProperties properties(boolean withFallback) {
		AppProperties.Llm.LlmProfile primary = new AppProperties.Llm.LlmProfile(
				"ollama", "fake-model", 0.5, 1000, 30, null, withFallback ? "fallback" : null);
		AppProperties.Llm.LlmProfile fallback = new AppProperties.Llm.LlmProfile(
				"ollama", "fallback-model", 0.5, 1000, 30, null, null);
		return new AppProperties(
				null, null, null, null, null, null, null, null,
				new AppProperties.Llm(
						BigDecimal.ZERO,
						Map.of("ideation", primary, "scoring", primary, "chat", primary, "finalize", primary,
								"fallback", fallback),
						Map.of("ollama", new AppProperties.Llm.ProviderConfig(null, "http://localhost:11434")),
						new AppProperties.Llm.Pricing(BigDecimal.ZERO, BigDecimal.ZERO, Map.of(), Map.of())),
				null,
				null);
	}

	/** Configurable fake ChatModel. */
	static class StubChatModel implements ChatModel {

		private final Queue<String> replies = new ConcurrentLinkedQueue<>();
		private final Deque<Function<ChatRequest, String>> responders = new ArrayDeque<>();
		final AtomicInteger calls = new AtomicInteger();

		void reply(String text) {
			replies.add(text);
		}

		void respond(Function<ChatRequest, String> responder) {
			responders.add(responder);
		}

		@Override
		public ChatResponse chat(ChatRequest request) {
			calls.incrementAndGet();
			String text;
			if (!responders.isEmpty()) {
				text = responders.poll().apply(request);
			} else {
				text = replies.poll();
			}
			if (text == null) {
				throw new IllegalStateException("StubChatModel has no reply queued");
			}
			return ChatResponse.builder()
					.aiMessage(new AiMessage(text))
					.tokenUsage(new TokenUsage(10, 5))
					.build();
		}

		@Override
		public ModelProvider provider() {
			return ModelProvider.OLLAMA;
		}
	}
}
