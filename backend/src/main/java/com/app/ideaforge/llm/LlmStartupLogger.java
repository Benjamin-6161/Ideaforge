package com.app.ideaforge.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.app.ideaforge.common.config.AppProperties;

/**
 * Logs the resolved LLM configuration at startup (never key values) and warns
 * when a selected provider is missing its key or base URL. This makes
 * "wrong provider / missing model" misconfigurations obvious immediately.
 */
@Component
public class LlmStartupLogger implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(LlmStartupLogger.class);

	private static final Map<String, String> KEY_ENV_VARS = Map.of(
			"anthropic", "ANTHROPIC_API_KEY",
			"openai", "OPENAI_API_KEY",
			"gemini", "GEMINI_API_KEY",
			"deepseek", "DEEPSEEK_API_KEY");

	private final AppProperties properties;

	public LlmStartupLogger(AppProperties properties) {
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		var profiles = properties.llm().profiles();
		if (profiles == null || profiles.isEmpty()) {
			log.warn("No LLM profiles are configured; all AI features will fail");
			return;
		}
		List<String> failures = new ArrayList<>();
		profiles.forEach((name, profile) -> {
			String provider = profile.provider() == null ? "" : profile.provider();
			String model = profile.model() == null ? "" : profile.model();
			log.info("LLM profile '{}': provider={}, model={}{}",
					name,
					provider.isBlank() ? "(missing)" : provider,
					model.isBlank() ? "(missing)" : model,
					profile.fallbackProfile() == null ? "" : ", fallback=" + profile.fallbackProfile());

			if (model.isBlank()) {
				failures.add("profile '" + name + "' has no model");
			}
			if (provider.isBlank()) {
				failures.add("profile '" + name + "' has no provider");
				return;
			}
			AppProperties.Llm.ProviderConfig providerConfig = properties.llm().providers() == null
					? null
					: properties.llm().providers().get(provider);
			String key = providerConfig == null ? null : providerConfig.apiKey();
			String baseUrl = providerConfig == null ? null : providerConfig.baseUrl();
			String expectedKeyEnv = KEY_ENV_VARS.get(provider);
			if (expectedKeyEnv != null && isBlank(key)) {
				failures.add("provider '" + provider + "' (profile '" + name + "') has no API key; set "
						+ expectedKeyEnv);
			}
			if (provider.equals("openai-compatible") && isBlank(baseUrl)
					&& isBlank(profile.baseUrl())) {
				failures.add("provider 'openai-compatible' (profile '" + name + "') needs "
						+ "OPENAI_COMPATIBLE_BASE_URL");
			}
		});
		if (!failures.isEmpty()) {
			log.warn("LLM configuration problems: {}", String.join("; ", failures));
		}
		Set<String> usedProviders = profiles.values().stream()
				.map(profile -> profile.provider() == null ? "" : profile.provider())
				.filter(provider -> !provider.isBlank())
				.collect(java.util.stream.Collectors.toSet());
		logProviderState(usedProviders);
	}

	private void logProviderState(Set<String> usedProviders) {
		if (properties.llm().providers() == null) {
			return;
		}
		List<String> lines = new ArrayList<>();
		properties.llm().providers().forEach((name, config) -> {
			if (!usedProviders.contains(name) && !name.equals("ollama")) {
				return;
			}
			String keyState = isBlank(config.apiKey()) ? "no key" : "key set";
			String endpoint = isBlank(config.baseUrl()) ? "" : ", baseUrl=" + config.baseUrl();
			lines.add(name + " (" + keyState + endpoint + ")");
		});
		log.info("LLM providers in use: {}", String.join(", ", lines));
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
