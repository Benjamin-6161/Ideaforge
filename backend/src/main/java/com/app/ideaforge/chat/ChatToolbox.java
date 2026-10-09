package com.app.ideaforge.chat;

import java.util.List;

import org.springframework.stereotype.Component;

import com.app.ideaforge.chat.tools.CommunitySearchTool;
import com.app.ideaforge.chat.tools.FetchPageTool;
import com.app.ideaforge.chat.tools.WebSearchTool;
import com.app.ideaforge.llm.LlmTool;
import com.app.ideaforge.research.CommunitySignalProvider;
import com.app.ideaforge.research.PageFetcher;
import com.app.ideaforge.research.SearchCacheService;
import com.app.ideaforge.research.SearchProvider;

import tools.jackson.databind.ObjectMapper;

/** The read-only tool set available to the chat agent. */
@Component
public class ChatToolbox {

	private final List<LlmTool> tools;

	public ChatToolbox(
			SearchProvider searchProvider,
			SearchCacheService searchCache,
			CommunitySignalProvider communitySignalProvider,
			PageFetcher pageFetcher,
			ObjectMapper objectMapper) {
		this.tools = List.of(
				new WebSearchTool(searchProvider, searchCache, objectMapper),
				new CommunitySearchTool(communitySignalProvider, objectMapper),
				new FetchPageTool(pageFetcher, objectMapper));
	}

	public List<LlmTool> tools() {
		return tools;
	}
}
