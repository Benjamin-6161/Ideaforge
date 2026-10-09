package com.app.ideaforge.ideas.dto;

import java.time.Instant;
import java.util.UUID;

public record IdeaDetail(
		UUID id,
		UUID fieldId,
		String fieldName,
		String niche,
		String title,
		String tagline,
		String status,
		boolean favorite,
		boolean archived,
		UUID currentVersionId,
		int versionNumber,
		Instant createdAt,
		Instant updatedAt,
		Instant finalizedAt,
		IdeaContent content,
		ValidationDetail validation,
		RevalidationInfo revalidation) {
}
