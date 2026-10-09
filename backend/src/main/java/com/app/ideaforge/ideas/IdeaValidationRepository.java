package com.app.ideaforge.ideas;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdeaValidationRepository extends JpaRepository<IdeaValidation, UUID> {

	Optional<IdeaValidation> findFirstByIdeaIdOrderByCreatedAtDesc(UUID ideaId);

	List<IdeaValidation> findByIdeaIdInOrderByCreatedAtDesc(java.util.Collection<UUID> ideaIds);
}
