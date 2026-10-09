package com.app.ideaforge.chat.dto;

import com.app.ideaforge.ideas.dto.IdeaContent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Structured revision response: the full updated idea plus a summary. */
public record IdeaRevisionResponse(
		@NotBlank @Size(max = 160) String changeSummary,
		@NotNull @Valid IdeaContent idea) {
}
