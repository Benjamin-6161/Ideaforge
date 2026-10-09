package com.app.ideaforge.ideas.dto;

import java.time.Instant;

/** Status of the latest re-validation run, shown on the idea detail page. */
public record RevalidationInfo(String status, String errorCode, Instant startedAt, Instant completedAt) {
}
