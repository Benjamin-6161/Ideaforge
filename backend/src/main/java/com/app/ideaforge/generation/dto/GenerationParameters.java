package com.app.ideaforge.generation.dto;

import java.util.List;

import com.app.ideaforge.generation.domain.AiCapability;
import com.app.ideaforge.generation.domain.AiDepth;
import com.app.ideaforge.generation.domain.BuilderProfile;
import com.app.ideaforge.generation.domain.CustomerSize;
import com.app.ideaforge.generation.domain.Monetization;
import com.app.ideaforge.generation.domain.NoveltyPreference;
import com.app.ideaforge.generation.domain.Region;
import com.app.ideaforge.generation.domain.StartingBudget;
import com.app.ideaforge.generation.domain.TargetCustomer;
import com.app.ideaforge.generation.domain.TimeToMvp;

import jakarta.validation.constraints.Size;

/**
 * Optional generation parameters. Enums are validated by Jackson at the
 * boundary; free text is length-capped here and injection-filtered in the
 * service before any credits are spent.
 */
public record GenerationParameters(
		TargetCustomer targetCustomer,
		CustomerSize customerSize,
		BuilderProfile builderProfile,
		TimeToMvp timeToMvp,
		StartingBudget startingBudget,
		AiDepth aiDepth,
		@Size(max = 8) List<AiCapability> aiCapabilities,
		Monetization monetization,
		Region region,
		NoveltyPreference noveltyPreference,
		@Size(max = 200) String problemFocus,
		@Size(max = 200) String exclude) {

	public GenerationParameters {
		aiCapabilities = aiCapabilities == null ? List.of() : List.copyOf(aiCapabilities);
	}

	public static GenerationParameters defaultParameters() {
		return new GenerationParameters(null, null, null, null, null, null, null, null, null, null, null, null);
	}

	public TargetCustomer targetCustomerOrDefault() {
		return targetCustomer == null ? TargetCustomer.ANY : targetCustomer;
	}

	public CustomerSize customerSizeOrDefault() {
		return customerSize == null ? CustomerSize.ANY : customerSize;
	}

	public BuilderProfile builderProfileOrDefault() {
		return builderProfile == null ? BuilderProfile.SOLO_FOUNDER : builderProfile;
	}

	public TimeToMvp timeToMvpOrDefault() {
		return timeToMvp == null ? TimeToMvp.NO_LIMIT : timeToMvp;
	}

	public StartingBudget startingBudgetOrDefault() {
		return startingBudget == null ? StartingBudget.MODERATE : startingBudget;
	}

	public AiDepth aiDepthOrDefault() {
		return aiDepth == null ? AiDepth.ANY : aiDepth;
	}

	public Monetization monetizationOrDefault() {
		return monetization == null ? Monetization.ANY : monetization;
	}

	public Region regionOrDefault() {
		return region == null ? Region.GLOBAL : region;
	}

	public NoveltyPreference noveltyPreferenceOrDefault() {
		return noveltyPreference == null ? NoveltyPreference.BALANCED : noveltyPreference;
	}
}
