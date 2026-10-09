package com.app.ideaforge.generation.dto;

import java.util.List;

import com.app.ideaforge.ideas.dto.Competitor;
import com.app.ideaforge.ideas.dto.ScoreRationales;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Shape of the batched scoring LLM response. */
public record ScoringResponse(
		@NotNull @Size(min = 1, max = 8) List<@Valid ScoredIdea> ideas) {

	public record ScoredIdea(
			@Min(0) int index,
			@Min(1) @Max(10) int demandScore,
			@Min(1) @Max(10) int profitabilityScore,
			@Min(1) @Max(10) int competitionScore,
			@Min(1) @Max(10) int noveltyScore,
			Boolean evidenceAvailable,
			@NotNull @Valid ScoreRationales rationales,
			@Size(max = 5) List<@Valid Competitor> competitors) {
	}
}
