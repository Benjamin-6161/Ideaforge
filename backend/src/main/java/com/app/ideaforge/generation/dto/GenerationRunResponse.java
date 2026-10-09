package com.app.ideaforge.generation.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Polling response for a generation run. */
public record GenerationRunResponse(
		UUID id,
		String status,
		String progressStage,
		String errorCode,
		UUID fieldId,
		String niche,
		Instant createdAt,
		Instant completedAt,
		List<GeneratedIdeaSummary> ideas) {

	public record GeneratedIdeaSummary(
			UUID id,
			String title,
			String tagline,
			UUID fieldId,
			String status,
			UUID currentVersionId,
			int versionNumber,
			Scores scores) {
	}

	public record Scores(
			int demand,
			int profitability,
			int competition,
			int novelty,
			boolean evidenceAvailable,
			String depth,
			UUID versionId) {
	}
}
