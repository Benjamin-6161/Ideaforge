package com.app.ideaforge.ideas.dto;

import java.util.UUID;

/** Scores summary used in lists and cards. */
public record ScoreSummary(
		int demand,
		int profitability,
		int competition,
		int novelty,
		boolean evidenceAvailable,
		String depth,
		UUID versionId) {
}
