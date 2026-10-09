package com.app.ideaforge.finalization.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Structured finalization output (stored in finalizations.output). */
public record FinalizePlan(
		@NotNull @Valid TechStack techStack,
		@NotNull @Size(min = 3, max = 6) List<@Valid RoadmapPhase> mvpRoadmap,
		@NotBlank @Size(max = 600) String pricingSuggestion,
		@NotNull @Size(min = 6, max = 16) List<@NotBlank @Size(max = 300) String> launchChecklist,
		@NotNull @Size(min = 3, max = 8) List<@NotBlank @Size(max = 300) String> validationNextSteps,
		@NotNull @Valid Branding branding,
		@NotNull @Size(min = 3, max = 10) List<@NotBlank @Size(max = 400) String> risks) {

	public record TechStack(
			@NotBlank @Size(max = 200) String frontend,
			@NotBlank @Size(max = 200) String backend,
			@NotBlank @Size(max = 200) String database,
			@NotBlank @Size(max = 200) String auth,
			@NotBlank @Size(max = 200) String hosting,
			@NotBlank @Size(max = 300) String aiLayer,
			@NotNull @Size(max = 8) List<@NotBlank @Size(max = 160) String> thirdParty,
			@NotBlank @Size(max = 100) String estMonthlyCostAtMvp,
			@NotBlank @Size(max = 700) String rationale) {
	}

	public record RoadmapPhase(
			@NotBlank @Size(max = 120) String phase,
			@NotNull @Size(min = 1, max = 8) List<@NotBlank @Size(max = 300) String> goals,
			@NotBlank @Size(max = 60) String estWeeks) {
	}

	public record Branding(
			@NotNull @Size(min = 3, max = 8) List<@NotBlank @Size(max = 80) String> nameIdeas,
			@NotBlank @Size(max = 400) String domainTips) {
	}
}
