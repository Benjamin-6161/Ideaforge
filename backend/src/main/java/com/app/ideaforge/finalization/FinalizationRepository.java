package com.app.ideaforge.finalization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinalizationRepository extends JpaRepository<Finalization, UUID> {

	Optional<Finalization> findByIdeaId(UUID ideaId);

	Optional<Finalization> findBySpendLedgerId(UUID spendLedgerId);

	List<Finalization> findByStatusIn(java.util.Collection<Finalization.Status> statuses);

	@Query("""
			select count(f) > 0 from Finalization f
			where f.status = 'RUNNING' and f.ideaId in (select i.id from Idea i where i.userId = :userId)
			""")
	boolean existsRunningForUser(@Param("userId") UUID userId);
}
