package com.app.ideaforge.research;

import java.time.Instant;

/** A demand/competition signal from a developer/business community. */
public record CommunitySignal(
		String title,
		String url,
		int points,
		int comments,
		Instant createdAt) {
}
