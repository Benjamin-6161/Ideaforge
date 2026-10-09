package com.app.ideaforge.ideas.dto;

import java.time.Instant;
import java.util.UUID;

import com.app.ideaforge.ideas.IdeaVersion;

public record VersionSummary(
		UUID id,
		int versionNumber,
		String source,
		String changeSummary,
		Instant createdAt,
		IdeaContent content) {

	public static VersionSummary from(IdeaVersion version, boolean includeContent) {
		return new VersionSummary(
				version.getId(),
				version.getVersionNumber(),
				version.getSource().name(),
				version.getChangeSummary(),
				version.getCreatedAt(),
				includeContent ? version.getContent() : null);
	}
}
