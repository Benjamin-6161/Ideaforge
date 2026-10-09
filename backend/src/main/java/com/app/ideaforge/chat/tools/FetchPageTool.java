package com.app.ideaforge.chat.tools;

import java.util.List;
import java.util.Map;

import com.app.ideaforge.llm.LlmTool;
import com.app.ideaforge.research.FetchedPage;
import com.app.ideaforge.research.PageFetcher;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Read-only, SSRF-safe single page fetch. */
public class FetchPageTool implements LlmTool {

	private static final int TEXT_CHARS = 4000;

	private final PageFetcher pageFetcher;
	private final ObjectMapper objectMapper;

	public FetchPageTool(PageFetcher pageFetcher, ObjectMapper objectMapper) {
		this.pageFetcher = pageFetcher;
		this.objectMapper = objectMapper;
	}

	@Override
	public String name() {
		return "fetch_page";
	}

	@Override
	public String description() {
		return "Fetch one public web page and extract its text (max ~4000 chars). Private and file URLs are blocked.";
	}

	@Override
	public List<ToolParameter> parameters() {
		return List.of(new ToolParameter("url", "string", "Public http(s) URL", true));
	}

	@Override
	public String execute(String jsonArguments) {
		try {
			Map<String, Object> args = objectMapper.readValue(jsonArguments, new TypeReference<>() {
			});
			String url = String.valueOf(args.getOrDefault("url", "")).trim();
			if (url.isBlank()) {
				return "{\"error\":\"url is required\"}";
			}
			FetchedPage page = pageFetcher.fetch(url);
			String text = page.text() == null ? "" : page.text();
			if (text.length() > TEXT_CHARS) {
				text = text.substring(0, TEXT_CHARS);
			}
			return objectMapper.writeValueAsString(Map.of(
					"url", page.url(),
					"title", page.title() == null ? "" : page.title(),
					"text", text));
		} catch (Exception ex) {
			return "{\"error\":\"fetch failed or URL blocked\"}";
		}
	}
}
