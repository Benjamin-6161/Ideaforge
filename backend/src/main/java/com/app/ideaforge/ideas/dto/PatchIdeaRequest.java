package com.app.ideaforge.ideas.dto;

/** Partial update; null fields are left unchanged. */
public record PatchIdeaRequest(Boolean isFavorite, Boolean isArchived) {
}
