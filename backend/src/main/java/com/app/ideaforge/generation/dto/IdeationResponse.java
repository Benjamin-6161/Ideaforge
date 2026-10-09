package com.app.ideaforge.generation.dto;

import java.util.List;
import java.util.UUID;

import com.app.ideaforge.ideas.dto.IdeaContent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Shape of the ideation LLM response. */
public record IdeationResponse(
		@NotNull @Size(min = 1, max = 8) List<@Valid IdeaContent> ideas) {
}
