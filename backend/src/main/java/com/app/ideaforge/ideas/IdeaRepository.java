package com.app.ideaforge.ideas;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdeaRepository extends JpaRepository<Idea, UUID> {

	Optional<Idea> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

	boolean existsByGenerationRunId(UUID generationRunId);

	List<Idea> findByGenerationRunIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID generationRunId);

	@Query("""
			select i.title from Idea i
			where i.userId = :userId and i.fieldId = :fieldId and i.deletedAt is null
			order by i.createdAt desc
			""")
	List<String> findRecentTitles(@Param("userId") UUID userId, @Param("fieldId") UUID fieldId, Pageable pageable);

	List<Idea> findByIdInAndUserIdAndDeletedAtIsNull(Collection<UUID> ids, UUID userId);
}
