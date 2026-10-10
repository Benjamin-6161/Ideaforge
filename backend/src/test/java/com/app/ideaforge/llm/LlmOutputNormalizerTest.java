package com.app.ideaforge.llm;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class LlmOutputNormalizerTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
	private final LlmOutputNormalizer normalizer = new LlmOutputNormalizer(new ObjectMapper());

	record TextHolder(@Size(max = 10) String value) {
	}

	record ListHolder(@Size(max = 2) List<@NotBlank String> items) {
	}

	@Test
	void truncatesOverLongStrings() {
		TextHolder value = new TextHolder("this is far too long to fit");
		Set<ConstraintViolation<TextHolder>> violations = validator.validate(value);

		TextHolder normalized = normalizer.normalize(value, violations);

		assertThat(normalized.value()).hasSizeLessThanOrEqualTo(10);
		assertThat(validator.validate(normalized)).isEmpty();
	}

	@Test
	void trimsOverLongListsToMax() {
		ListHolder value = new ListHolder(List.of("a", "b", "c", "d"));
		Set<ConstraintViolation<ListHolder>> violations = validator.validate(value);

		ListHolder normalized = normalizer.normalize(value, violations);

		assertThat(normalized.items()).hasSize(2);
		assertThat(validator.validate(normalized)).isEmpty();
	}

	@Test
	void leavesNonRangeViolationsAlone() {
		TextHolder blank = new TextHolder("");
		Set<ConstraintViolation<TextHolder>> violations = validator.validate(blank);
		// No constraint is violated here (blank is allowed by @Size alone).
		assertThat(violations).isEmpty();

		record RequiredHolder(@NotBlank String value) {
		}
		RequiredHolder value = new RequiredHolder("");
		Set<ConstraintViolation<RequiredHolder>> required = validator.validate(value);
		RequiredHolder normalized = normalizer.normalize(value, required);
		assertThat(normalized.value()).isEmpty();
	}

	@Test
	void truncationIsSurrogateSafe() {
		assertThat(LlmOutputNormalizer.truncate("abcdefghij\uD83D\uDE00", 11)).isEqualTo("abcdefghij");
		assertThat(LlmOutputNormalizer.truncate("short", 10)).isEqualTo("short");
		assertThat(LlmOutputNormalizer.truncate("trailing   ", 20)).isEqualTo("trailing   ");
	}
}
