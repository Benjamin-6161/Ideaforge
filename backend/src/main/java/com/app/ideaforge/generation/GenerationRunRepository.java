package com.app.ideaforge.generation;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GenerationRunRepository extends JpaRepository<GenerationRun, UUID> {

	Optional<GenerationRun> findByIdAndUserId(UUID id, UUID userId);

	Optional<GenerationRun> findBySpendLedgerId(UUID spendLedgerId);

	boolean existsByUserIdAndStatusIn(UUID userId, Collection<RunStatus> statuses);

	long countByUserIdAndCreatedAtAfter(UUID userId, Instant after);

	List<GenerationRun> findByStatusIn(Collection<RunStatus> statuses);

	/** Serializes generation creation per user (transaction-scoped advisory lock). */
	@Query(value = "select pg_advisory_xact_lock(hashtext(cast(:userId as text)))", nativeQuery = true)
	void lockUser(@Param("userId") UUID userId);
}
