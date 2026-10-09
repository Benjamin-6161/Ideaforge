package com.app.ideaforge.finalization.dto;

import java.time.Instant;
import java.util.UUID;

public record FinalizationResponse(
		UUID id,
		UUID ideaId,
		UUID versionId,
		String status,
		String errorCode,
		FinalizePlan output,
		String buildPromptMd,
		String agentsMd,
		String promptVersion,
		Instant createdAt,
		Instant completedAt) {
}
