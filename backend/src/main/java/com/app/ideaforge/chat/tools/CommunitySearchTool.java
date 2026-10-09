package com.app.ideaforge.chat.tools;

import java.util.List;
import java.util.Map;

import com.app.ideaforge.llm.LlmTool;
import com.app.ideaforge.research.CommunitySignal;
import com.app.ideaforge.research.CommunitySignalProvider;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Read-only Hacker News search for community demand signals. */
public class CommunitySearchTool implements LlmTool {

	private final CommunitySignalProvider communityProvider;
	private final ObjectMapper objectMapper;

	public CommunitySearchTool(CommunitySignalProvider communityProvider, ObjectMapper objectMapper) {
		this.communityProvider = communityProvider;
		this.objectMapper = objectMapper;
	}

	@Override
	public String name() {
		return "hacker_news";
	}

	@Override
	public String description() {
		return "Search Hacker News stories for demand and competition signals. Returns title, URL, points, comments.";
	}

	@Override
	public List<ToolParameter> parameters() {
		return List.of(new ToolParameter("query", "string", "Search query", true));
	}

	@Override
	public String execute(String jsonArguments) {
		try {
			Map<String, Object> args = objectMapper.readValue(jsonArguments, new TypeReference<>() {
			});
			String query = String.valueOf(args.getOrDefault("query", "")).trim();
			if (query.isBlank()) {
				return "{\"error\":\"query is required\"}";
			}
			List<CommunitySignal> signals = communityProvider.search(query, 5);
			List<Map<String, Object>> payload = signals.stream()
					.map(signal -> Map.<String, Object>of(
							"title", ToolText.sanitize(signal.title(), 300),
							"url", signal.url(),
							"points", signal.points(),
							"comments", signal.comments()))
					.toList();
			return objectMapper.writeValueAsString(payload);
		} catch (Exception ex) {
			return "{\"error\":\"community search failed\"}";
		}
	}
}
