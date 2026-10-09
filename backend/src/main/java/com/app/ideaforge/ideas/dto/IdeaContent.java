package com.app.ideaforge.ideas.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Schema v1 of an idea's content. The LLM response is validated against these
 * constraints (lengths, list sizes) before anything is persisted.
 */
public record IdeaContent(
		@NotBlank @Size(max = 80) String title,
		@NotBlank @Size(max = 140) String tagline,
		@NotBlank @Size(max = 500) String summary,
		@NotNull @Valid Problem problem,
		@NotNull @Valid TargetCustomer targetCustomer,
		@NotNull @Valid Solution solution,
		@NotNull @Valid AiRole aiRole,
		@NotNull @Valid Differentiation differentiation,
		@NotNull @Valid Monetization monetization,
		@NotNull @Valid GoToMarket goToMarket,
		@NotNull @Valid BuildEstimate buildEstimate,
		@NotNull @Size(min = 3, max = 6) List<@NotBlank @Size(max = 400) String> caveats,
		@NotNull List<@NotBlank @Size(max = 400) String> regulatoryNotes,
		@NotNull List<@NotBlank @Size(max = 400) String> openQuestions) {

	public record Problem(
			@NotBlank @Size(max = 600) String statement,
			@NotBlank @Size(max = 300) String whoFeelsIt,
			@NotBlank @Size(max = 400) String currentWorkaround,
			@NotBlank @Size(max = 200) String painSeverity,
			@NotBlank @Size(max = 200) String frequency) {
	}

	public record TargetCustomer(
			@NotBlank @Size(max = 300) String persona,
			@NotBlank @Size(max = 200) String segment,
			@NotBlank @Size(max = 100) String size,
			@NotBlank @Size(max = 200) String buyer) {
	}

	public record Solution(
			@NotBlank @Size(max = 800) String overview,
			@NotNull @Size(min = 3, max = 6) List<@Valid KeyFeature> keyFeatures,
			@NotNull @Size(min = 3, max = 6) List<@NotBlank @Size(max = 300) String> mvpScope) {
	}

	public record KeyFeature(
			@NotBlank @Size(max = 80) String name,
			@NotBlank @Size(max = 400) String description) {
	}

	public record AiRole(
			@NotBlank @Size(max = 600) String summary,
			@NotNull @Size(min = 1, max = 8) List<@NotBlank @Size(max = 120) String> capabilities,
			@NotBlank @Size(max = 600) String whyAiIsEssential,
			@NotBlank @Size(max = 400) String dataNeeded,
			@NotBlank @Size(max = 400) String accuracyRisks) {
	}

	public record Differentiation(
			@NotBlank @Size(max = 500) String wedge,
			@NotBlank @Size(max = 500) String moat,
			@NotBlank @Size(max = 500) String whyNow) {
	}

	public record Monetization(
			@NotBlank @Size(max = 200) String model,
			@NotBlank @Size(max = 300) String pricingExample,
			@NotBlank @Size(max = 400) String willingnessToPayNotes) {
	}

	public record GoToMarket(
			@NotBlank @Size(max = 500) String firstCustomers,
			@NotNull @Size(min = 1, max = 6) List<@NotBlank @Size(max = 200) String> channels) {
	}

	public record BuildEstimate(
			@NotBlank @Size(max = 100) String complexity,
			@NotBlank @Size(max = 100) String mvpTimeEstimate,
			@NotBlank @Size(max = 200) String suggestedTeam) {
	}
}
