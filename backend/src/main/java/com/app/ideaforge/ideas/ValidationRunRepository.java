package com.app.ideaforge.ideas;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidationRunRepository extends JpaRepository<ValidationRun, UUID> {

	Optional<ValidationRun> findBySpendLedgerId(UUID spendLedgerId);

	Optional<ValidationRun> findFirstByIdeaIdOrderByCreatedAtDesc(UUID ideaId);

	List<ValidationRun> findByStatusIn(java.util.Collection<ValidationRun.Status> statuses);

	boolean existsByIdeaIdAndStatus(UUID ideaId, ValidationRun.Status status);
}
