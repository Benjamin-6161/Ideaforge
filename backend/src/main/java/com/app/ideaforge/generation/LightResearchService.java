package com.app.ideaforge.generation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.app.ideaforge.ideas.dto.EvidenceItem;
import com.app.ideaforge.research.CommunitySignal;
import com.app.ideaforge.research.CommunitySignalProvider;
import com.app.ideaforge.research.SearchCacheService;
import com.app.ideaforge.research.SearchProvider;
import com.app.ideaforge.ideas.dto.IdeaContent;

/**
 * Cheap light check: per idea at most two searches (competitors, pain/demand)
 * plus one community query. Snippets only; no page fetches. Never throws.
 */
@Service
public class LightResearchService {

	private static final int RESULTS_PER_QUERY = 4;
	private static final int COMMUNITY_RESULTS = 3;
	private static final int MAX_EVIDENCE_PER_IDEA = 8;
	private static final int SNIPPET_CHARS = 300;

	private final SearchProvider searchProvider;
	private final CommunitySignalProvider communityProvider;
	private final SearchCacheService searchCache;

	public LightResearchService(
			SearchProvider searchProvider,
			CommunitySignalProvider communityProvider,
			SearchCacheService searchCache) {
		this.searchProvider = searchProvider;
		this.communityProvider = communityProvider;
		this.searchCache = searchCache;
	}

	public ResearchBundle research(IdeaContent idea) {
		List<String> queries = List.of(
				truncate(idea.title() + " " + idea.targetCustomer().segment() + " competitors", 200),
				truncate(idea.problem().statement() + " " + idea.targetCustomer().persona(), 200));

		List<EvidenceItem> evidence = new ArrayList<>();
		for (String query : queries) {
			List<com.app.ideaforge.research.SearchResult> results = searchCache.getOrLoad(
					"searxng", query, () -> searchProvider.search(query, RESULTS_PER_QUERY));
			for (var result : results) {
				evidence.add(new EvidenceItem(
						"search",
						truncate(result.title(), 300),
						truncate(result.url(), 2000),
						truncate(result.snippet(), SNIPPET_CHARS)));
			}
		}

		String communityQuery = truncate(idea.title(), 150);
		List<CommunitySignal> signals = communityProvider.search(communityQuery, COMMUNITY_RESULTS);
		for (CommunitySignal signal : signals) {
			evidence.add(new EvidenceItem(
					"community",
					truncate(signal.title(), 300),
					truncate(signal.url(), 2000),
					"points: " + signal.points() + ", comments: " + signal.comments()));
		}

		return new ResearchBundle(dedupeAndCap(evidence), queries);
	}

	private List<EvidenceItem> dedupeAndCap(List<EvidenceItem> evidence) {
		Map<String, EvidenceItem> byUrl = new LinkedHashMap<>();
		for (EvidenceItem item : evidence) {
			byUrl.putIfAbsent(item.url(), item);
		}
		return byUrl.values().stream().limit(MAX_EVIDENCE_PER_IDEA).toList();
	}

	private String truncate(String value, int max) {
		if (value == null) {
			return null;
		}
		return value.length() <= max ? value : value.substring(0, max);
	}
}
