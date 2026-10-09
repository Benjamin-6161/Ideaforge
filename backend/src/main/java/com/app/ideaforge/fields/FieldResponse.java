package com.app.ideaforge.fields;

import java.util.List;
import java.util.UUID;

public record FieldResponse(
		UUID id,
		UUID parentId,
		String slug,
		String name,
		String description,
		String icon,
		List<FieldResponse> niches) {

	public static FieldResponse from(Field field, List<Field> children) {
		return new FieldResponse(
				field.getId(),
				field.getParentId(),
				field.getSlug(),
				field.getName(),
				field.getDescription(),
				field.getIcon(),
				children.stream().map(child -> from(child, List.of())).toList());
	}
}
