package com.app.ideaforge.research;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.app.ideaforge.ideas.dto.Competitor;
import com.app.ideaforge.ideas.dto.EvidenceItem;
import com.app.ideaforge.ideas.dto.IdeaContent;

/**
 * Deep check: up to eight searches, a community query, optional trends, and up
 * to five fetched competitor pages. Every step is best-effort; failures are
 * skipped and never fail the whole request.
 */
@Service
public class DeepResearchService {

	private static final int RESULTS_PER_QUERY = 4;
	private static final int MAX_FETCHED_PAGES = 5;
	private static final int MAX_EVIDENCE = 24;
	private static final int SNIPPET_CHARS = 400;
	private static final int PAGE_EXCERPT_CHARS = 600;

	private final SearchProvider searchProvider;
	private final CommunitySignalProvider communityProvider;
	private final SearchCacheService searchCache;
	private final PageFetcher pageFetcher;
	private final TrendsProvider trendsProvider;

	public DeepResearchService(
			SearchProvider searchProvider,
			CommunitySignalProvider communityProvider,
			SearchCacheService searchCache,
			PageFetcher pageFetcher,
			TrendsProvider trendsProvider) {
		this.searchProvider = searchProvider;
		this.communityProvider = communityProvider;
		this.searchCache = searchCache;
		this.pageFetcher = pageFetcher;
		this.trendsProvider = trendsProvider;
	}

	public record DeepBundle(
			List<EvidenceItem> evidence,
			List<Competitor> fetchedCompetitors,
			List<String> queries) {
	}

	public DeepBundle research(IdeaContent idea) {
		List<String> queries = List.of(
				truncate(idea.title() + " competitors " + idea.targetCustomer().segment(), 200),
				truncate(idea.problem().statement() + " software", 200),
				truncate(idea.title() + " pricing", 200),
				truncate(idea.targetCustomer().persona() + " " + idea.targetCustomer().segment() + " tools", 200));

		List<EvidenceItem> evidence = new ArrayList<>();
		for (String query : queries) {
			List<SearchResult> results = searchCache.getOrLoad(
					"searxng", query, () -> searchProvider.search(query, RESULTS_PER_QUERY));
			for (SearchResult result : results) {
				evidence.add(new EvidenceItem(
						"search",
						truncate(result.title(), 300),
						truncate(result.url(), 2000),
						truncate(result.snippet(), SNIPPET_CHARS)));
			}
		}

		for (CommunitySignal signal : communityProvider.search(truncate(idea.title(), 150), 5)) {
			evidence.add(new EvidenceItem(
					"community",
					truncate(signal.title(), 300),
					truncate(signal.url(), 2000),
					"points: " + signal.points() + ", comments: " + signal.comments()));
		}

		if (trendsProvider.isEnabled()) {
			var points = trendsProvider.interest(truncate(idea.title(), 100), null);
			if (!points.isEmpty()) {
				evidence.add(new EvidenceItem(
						"trends",
						"Interest over time: " + truncate(idea.title(), 200),
						"https://trends.google.com/",
						"latest value: " + points.get(points.size() - 1).value()));
			}
		}

		List<Competitor> fetched = fetchTopPages(evidence);
		return new DeepBundle(dedupeAndCap(evidence), fetched, queries);
	}

	private List<Competitor> fetchTopPages(List<EvidenceItem> evidence) {
		List<Competitor> competitors = new ArrayList<>();
		List<EvidenceItem> pageEvidence = new ArrayList<>();
		Map<String, Boolean> seen = new LinkedHashMap<>();
		for (EvidenceItem item : List.copyOf(evidence)) {
			if (competitors.size() >= MAX_FETCHED_PAGES || !"search".equals(item.kind())) {
				continue;
			}
			if (seen.putIfAbsent(item.url(), Boolean.TRUE) != null) {
				continue;
			}
			try {
				FetchedPage page = pageFetcher.fetch(item.url());
				pageEvidence.add(new EvidenceItem(
						"page",
						truncate(page.title() == null ? item.title() : page.title(), 300),
						truncate(page.url(), 2000),
						truncate(page.text(), PAGE_EXCERPT_CHARS)));
				competitors.add(new Competitor(
						truncate(page.title() == null || page.title().isBlank() ? item.title() : page.title(), 160),
						truncate(page.url(), 2000),
						truncate(excerpt(page.text()), 300)));
			} catch (ResearchException ex) {
				// Page fetch is best-effort; keep the snippet evidence only.
			}
		}
		evidence.addAll(pageEvidence);
		return competitors;
	}

	private List<EvidenceItem> dedupeAndCap(List<EvidenceItem> evidence) {
		// LinkedHashMap.put keeps the first position but lets later (richer) page
		// evidence replace an earlier snippet for the same URL.
		Map<String, EvidenceItem> byUrl = new LinkedHashMap<>();
		for (EvidenceItem item : evidence) {
			byUrl.put(item.url(), item);
		}
		return byUrl.values().stream().limit(MAX_EVIDENCE).toList();
	}

	private String excerpt(String text) {
		if (text == null || text.isBlank()) {
			return "No text extracted";
		}
		int end = Math.min(text.length(), 200);
		return text.substring(0, end);
	}

	private String truncate(String value, int max) {
		if (value == null) {
			return "";
		}
		return value.length() <= max ? value : value.substring(0, max);
	}
}
