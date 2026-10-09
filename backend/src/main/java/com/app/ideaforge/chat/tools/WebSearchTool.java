package com.app.ideaforge.chat.tools;

import java.util.List;
import java.util.Map;

import com.app.ideaforge.llm.LlmTool;
import com.app.ideaforge.research.SearchCacheService;
import com.app.ideaforge.research.SearchProvider;
import com.app.ideaforge.research.SearchResult;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Read-only web search over the cached SearXNG provider. */
public class WebSearchTool implements LlmTool {

	private static final int MAX_RESULTS = 6;
	private static final int SNIPPET_CHARS = 400;

	private final SearchProvider searchProvider;
	private final SearchCacheService searchCache;
	private final ObjectMapper objectMapper;

	public WebSearchTool(SearchProvider searchProvider, SearchCacheService searchCache, ObjectMapper objectMapper) {
		this.searchProvider = searchProvider;
		this.searchCache = searchCache;
		this.objectMapper = objectMapper;
	}

	@Override
	public String name() {
		return "web_search";
	}

	@Override
	public String description() {
		return "Search the web for competitors, pricing, and market context. Returns titles, URLs, and snippets.";
	}

	@Override
	public List<ToolParameter> parameters() {
		return List.of(
				new ToolParameter("query", "string", "Search query", true),
				new ToolParameter("max_results", "integer", "Maximum results (1-6, default 5)", false));
	}

	@Override
	public String execute(String jsonArguments) {
		try {
			Map<String, Object> args = objectMapper.readValue(jsonArguments, new TypeReference<>() {
			});
			String query = String.valueOf(args.getOrDefault("query", "")).trim();
			int max = args.get("max_results") instanceof Number number
					? Math.max(1, Math.min(MAX_RESULTS, number.intValue()))
					: 5;
			if (query.isBlank()) {
				return "{\"error\":\"query is required\"}";
			}
			List<SearchResult> results = searchCache.getOrLoad(
					"searxng", query, () -> searchProvider.search(query, max));
			List<Map<String, String>> payload = results.stream()
					.limit(max)
					.map(result -> Map.of(
							"title", truncate(result.title(), 200),
							"url", result.url(),
							"snippet", truncate(result.snippet(), SNIPPET_CHARS)))
					.toList();
			return objectMapper.writeValueAsString(payload);
		} catch (Exception ex) {
			return "{\"error\":\"search failed\"}";
		}
	}

	private String truncate(String value, int max) {
		if (value == null) {
			return "";
		}
		return value.length() <= max ? value : value.substring(0, max);
	}
}
