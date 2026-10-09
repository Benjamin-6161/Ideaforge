package com.app.ideaforge.llm;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.app.ideaforge.common.config.AppProperties;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * Builds one ChatModel per configured profile. Provider and model are config
 * only; this is the only class that knows concrete LangChain4j providers.
 */
@Component
public class LlmModelRegistry {

	private final AppProperties properties;
	private final Map<String, ChatModel> models = new ConcurrentHashMap<>();

	public LlmModelRegistry(AppProperties properties) {
		this.properties = properties;
	}

	public AppProperties.Llm.LlmProfile profile(String profileName) {
		AppProperties.Llm.LlmProfile profile = properties.llm().profiles().get(profileName);
		if (profile == null) {
			throw new LlmException("No LLM profile configured named '" + profileName + "'");
		}
		return profile;
	}

	public ChatModel modelFor(String profileName) {
		return models.computeIfAbsent(profileName, name -> build(name, profile(name)));
	}

	private ChatModel build(String profileName, AppProperties.Llm.LlmProfile profile) {
		String provider = profile.provider() == null ? "" : profile.provider().toLowerCase(Locale.ROOT);
		String model = requireModel(profileName, profile);
		Duration timeout = Duration.ofSeconds(profile.timeoutSeconds() == null ? 120 : profile.timeoutSeconds());
		AppProperties.Llm.ProviderConfig providerConfig = properties.llm().providers().get(provider);
		String apiKey = providerConfig == null ? null : providerConfig.apiKey();
		String baseUrl = firstNonBlank(profile.baseUrl(), providerConfig == null ? null : providerConfig.baseUrl());

		return switch (provider) {
			case "anthropic" -> AnthropicChatModel.builder()
					.apiKey(requireKey(provider, apiKey))
					.modelName(model)
					.timeout(timeout)
					.maxRetries(1)
					.build();
			case "openai" -> OpenAiChatModel.builder()
					.apiKey(requireKey(provider, apiKey))
					.modelName(model)
					.timeout(timeout)
					.maxRetries(1)
					.build();
			case "gemini" -> GoogleAiGeminiChatModel.builder()
					.apiKey(requireKey(provider, apiKey))
					.modelName(model)
					.timeout(timeout)
					.maxRetries(1)
					.build();
			case "ollama" -> OllamaChatModel.builder()
					.baseUrl(firstNonBlank(baseUrl, "http://localhost:11434"))
					.modelName(model)
					.timeout(timeout)
					.build();
			case "openai-compatible" -> OpenAiChatModel.builder()
					.apiKey(apiKey == null || apiKey.isBlank() ? "not-needed" : apiKey)
					.baseUrl(requireBaseUrl(profileName, baseUrl))
					.modelName(model)
					.timeout(timeout)
					.maxRetries(1)
					.build();
			default -> throw new LlmException(
					"Unknown LLM provider '" + provider + "' for profile '" + profileName + "'");
		};
	}

	private String requireModel(String profileName, AppProperties.Llm.LlmProfile profile) {
		if (profile.model() == null || profile.model().isBlank()) {
			throw new LlmException("No model configured for LLM profile '" + profileName + "'");
		}
		return profile.model();
	}

	private String requireKey(String provider, String apiKey) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new LlmException("Provider '" + provider + "' is not configured (missing API key)");
		}
		return apiKey;
	}

	private String requireBaseUrl(String profileName, String baseUrl) {
		if (baseUrl == null || baseUrl.isBlank()) {
			throw new LlmException("Profile '" + profileName + "' requires a base URL for openai-compatible");
		}
		return baseUrl;
	}

	private String firstNonBlank(String first, String second) {
		if (first != null && !first.isBlank()) {
			return first;
		}
		return second == null || second.isBlank() ? null : second;
	}
}
