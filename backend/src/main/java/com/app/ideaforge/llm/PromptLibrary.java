package com.app.ideaforge.llm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Loads versioned prompt files from `classpath:prompts/<name>-vN.md`. The
 * highest version wins; the label (e.g. `ideation-system-v1`) is stored on
 * runs and finalizations.
 */
@Component
public class PromptLibrary {

	private static final Pattern FILE_PATTERN = Pattern.compile("([a-z0-9-]+)-v(\\d+)\\.md$");

	private final Map<String, PromptTemplate> cache = new ConcurrentHashMap<>();

	public PromptTemplate load(String name) {
		return cache.computeIfAbsent(name, this::discover);
	}

	private PromptTemplate discover(String name) {
		try {
			Resource[] resources = new PathMatchingResourcePatternResolver()
					.getResources("classpath*:prompts/*.md");
			PromptTemplate best = null;
			for (Resource resource : resources) {
				String filename = resource.getFilename();
				if (filename == null) {
					continue;
				}
				Matcher matcher = FILE_PATTERN.matcher(filename);
				if (!matcher.matches() || !matcher.group(1).equals(name)) {
					continue;
				}
				int version = Integer.parseInt(matcher.group(2));
				if (best == null || version > best.version()) {
					String content = resource.getContentAsString(StandardCharsets.UTF_8);
					best = new PromptTemplate(name, version, name + "-v" + version, content);
				}
			}
			if (best == null) {
				throw new LlmException("No prompt file found for '" + name + "'");
			}
			return best;
		} catch (IOException ex) {
			throw new LlmException("Failed to load prompt '" + name + "'", ex);
		}
	}

	public record PromptTemplate(String name, int version, String versionLabel, String content) {
	}
}
