package com.app.ideaforge.generation.dto;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateGenerationRequest(
		@NotNull UUID fieldId,
		@Size(max = 100) String niche,
		@Valid GenerationParameters parameters) {

	public GenerationParameters parametersOrDefault() {
		return parameters == null ? GenerationParameters.defaultParameters() : parameters;
	}
}
