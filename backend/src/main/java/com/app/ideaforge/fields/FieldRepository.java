package com.app.ideaforge.fields;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldRepository extends JpaRepository<Field, UUID> {

	List<Field> findByActiveTrueAndParentIdIsNullOrderBySortOrderAsc();

	List<Field> findByActiveTrueAndParentIdIsNotNullOrderBySortOrderAsc();

	Optional<Field> findBySlug(String slug);

	Optional<Field> findByIdAndActiveTrue(UUID id);
}
