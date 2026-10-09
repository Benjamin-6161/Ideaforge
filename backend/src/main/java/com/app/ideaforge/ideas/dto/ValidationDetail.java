package com.app.ideaforge.ideas.dto;

import java.time.Instant;
import java.util.List;

/** Full validation payload for the idea detail page. */
public record ValidationDetail(
		ScoreSummary scores,
		ScoreRationales rationale,
		List<Competitor> competitors,
		List<EvidenceItem> evidence,
		List<String> queries,
		Instant createdAt,
		int validatedVersionNumber,
		boolean stale) {
}
