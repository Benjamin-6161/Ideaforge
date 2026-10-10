package com.app.ideaforge.llm;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.app.ideaforge.common.config.AppProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Provider/model selection is config-only: every provider builds from config. */
class LlmModelRegistryTest {

	@Test
	void buildsAModelForEveryConfiguredProvider() {
		AppProperties properties = properties(Map.of(
				"anthropic", profile("anthropic", "claude-test"),
				"openai", profile("openai", "gpt-test"),
				"gemini", profile("gemini", "gemini-test"),
				"deepseek", profile("deepseek", "deepseek-chat"),
				"ollama", profile("ollama", "llama-test"),
				"compatible", profile("openai-compatible", "custom-model")),
				Map.of(
						"anthropic", new AppProperties.Llm.ProviderConfig("test-key", null),
						"openai", new AppProperties.Llm.ProviderConfig("test-key", null),
						"gemini", new AppProperties.Llm.ProviderConfig("test-key", null),
						"deepseek", new AppProperties.Llm.ProviderConfig("test-key", "https://api.deepseek.com/v1"),
						"ollama", new AppProperties.Llm.ProviderConfig(null, "http://localhost:11434"),
						"openai-compatible", new AppProperties.Llm.ProviderConfig("test-key", "https://llm.example/v1")));
		LlmModelRegistry registry = new LlmModelRegistry(properties);

		for (String name : List.of("anthropic", "openai", "gemini", "deepseek", "ollama", "compatible")) {
			assertThat(registry.modelFor(name)).as("model for %s", name).isNotNull();
		}
	}

	@Test
	void deepseekDefaultsToItsPublicBaseUrl() {
		// No base URL configured: the provider falls back to the DeepSeek endpoint.
		AppProperties properties = properties(
				Map.of("deepseek", profile("deepseek", "deepseek-chat")),
				Map.of("deepseek", new AppProperties.Llm.ProviderConfig("test-key", null)));
		LlmModelRegistry registry = new LlmModelRegistry(properties);
		assertThat(LlmModelRegistry.DEEPSEEK_DEFAULT_BASE_URL).isEqualTo("https://api.deepseek.com/v1");
		assertThat(registry.modelFor("deepseek")).isNotNull();
	}

	@Test
	void missingDeepseekKeyIsNotConfigured() {
		AppProperties properties = properties(
				Map.of("deepseek", profile("deepseek", "deepseek-chat")),
				Map.of("deepseek", new AppProperties.Llm.ProviderConfig("", null)));
		LlmModelRegistry registry = new LlmModelRegistry(properties);
		assertThatThrownBy(() -> registry.modelFor("deepseek"))
				.isInstanceOf(LlmException.class)
				.satisfies(ex -> assertThat(((LlmException) ex).getCode())
						.isEqualTo(LlmErrorCode.NOT_CONFIGURED));
	}

	@Test
	void rejectsUnknownProvider() {
		AppProperties properties = properties(
				Map.of("weird", profile("martian-llm", "x")),
				Map.of());
		LlmModelRegistry registry = new LlmModelRegistry(properties);
		assertThatThrownBy(() -> registry.modelFor("weird"))
				.isInstanceOf(LlmException.class)
				.hasMessageContaining("Unknown LLM provider");
	}

	@Test
	void rejectsMissingApiKey() {
		AppProperties properties = properties(
				Map.of("anthropic", profile("anthropic", "claude-test")),
				Map.of("anthropic", new AppProperties.Llm.ProviderConfig("", null)));
		LlmModelRegistry registry = new LlmModelRegistry(properties);
		assertThatThrownBy(() -> registry.modelFor("anthropic"))
				.isInstanceOf(LlmException.class)
				.hasMessageContaining("not configured");
	}

	@Test
	void rejectsMissingModelName() {
		AppProperties properties = properties(
				Map.of("ollama", profile("ollama", "")),
				Map.of("ollama", new AppProperties.Llm.ProviderConfig(null, "http://localhost:11434")));
		LlmModelRegistry registry = new LlmModelRegistry(properties);
		assertThatThrownBy(() -> registry.modelFor("ollama"))
				.isInstanceOf(LlmException.class)
				.hasMessageContaining("No model configured");
	}

	private AppProperties.Llm.LlmProfile profile(String provider, String model) {
		return new AppProperties.Llm.LlmProfile(provider, model, 0.5, 1000, 60, null, null);
	}

	private AppProperties properties(
			Map<String, AppProperties.Llm.LlmProfile> profiles,
			Map<String, AppProperties.Llm.ProviderConfig> providers) {
		return new AppProperties(
				null, null, null, null, null, null, null, null,
				new AppProperties.Llm(
						java.math.BigDecimal.TEN,
						profiles,
						providers,
						new AppProperties.Llm.Pricing(
								java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, Map.of(), Map.of())),
				null,
				null);
	}
}
