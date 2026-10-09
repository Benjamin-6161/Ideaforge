package com.app.ideaforge.fields;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.ideaforge.common.error.ApiException;
import com.app.ideaforge.common.error.ErrorCodes;
import com.app.ideaforge.generation.domain.AiCapability;
import com.app.ideaforge.generation.domain.AiDepth;
import com.app.ideaforge.generation.domain.BuilderProfile;
import com.app.ideaforge.generation.domain.CustomerSize;
import com.app.ideaforge.generation.domain.LabeledOption;
import com.app.ideaforge.generation.domain.Monetization;
import com.app.ideaforge.generation.domain.NoveltyPreference;
import com.app.ideaforge.generation.domain.Region;
import com.app.ideaforge.generation.domain.StartingBudget;
import com.app.ideaforge.generation.domain.TargetCustomer;
import com.app.ideaforge.generation.domain.TimeToMvp;

@Service
public class FieldService {

	private final FieldRepository fields;

	public FieldService(FieldRepository fields) {
		this.fields = fields;
	}

	@Transactional(readOnly = true)
	public List<FieldResponse> listFieldsWithNiches() {
		List<Field> parents = fields.findByActiveTrueAndParentIdIsNullOrderBySortOrderAsc();
		Map<UUID, List<Field>> childrenByParent = fields.findByActiveTrueAndParentIdIsNotNullOrderBySortOrderAsc()
				.stream()
				.collect(Collectors.groupingBy(Field::getParentId));
		return parents.stream()
				.map(parent -> FieldResponse.from(parent, childrenByParent.getOrDefault(parent.getId(), List.of())))
				.toList();
	}

	@Transactional(readOnly = true)
	public Field requireActive(UUID id) {
		return fields.findByIdAndActiveTrue(id)
				.orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Field not found."));
	}

	public GenerationOptionsResponse generationOptions() {
		return new GenerationOptionsResponse(
				options(TargetCustomer.values()),
				options(CustomerSize.values()),
				options(BuilderProfile.values()),
				options(TimeToMvp.values()),
				options(StartingBudget.values()),
				options(AiDepth.values()),
				options(AiCapability.values()),
				options(Monetization.values()),
				options(Region.values()),
				options(NoveltyPreference.values()));
	}

	private <E extends Enum<E> & LabeledOption> List<GenerationOptionsResponse.OptionValue> options(E[] values) {
		return java.util.Arrays.stream(values)
				.map(value -> new GenerationOptionsResponse.OptionValue(value.name(), value.label()))
				.toList();
	}

	public record GenerationOptionsResponse(
			List<OptionValue> targetCustomer,
			List<OptionValue> customerSize,
			List<OptionValue> builderProfile,
			List<OptionValue> timeToMvp,
			List<OptionValue> startingBudget,
			List<OptionValue> aiDepth,
			List<OptionValue> aiCapabilities,
			List<OptionValue> monetization,
			List<OptionValue> region,
			List<OptionValue> noveltyPreference) {

		public record OptionValue(String value, String label) {
		}
	}
}
