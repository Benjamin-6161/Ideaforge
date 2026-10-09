package com.app.ideaforge.ideas;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdeaVersionRepository extends JpaRepository<IdeaVersion, UUID> {

	Optional<IdeaVersion> findByIdAndIdeaId(UUID id, UUID ideaId);

	@Query("select coalesce(max(v.versionNumber), 0) from IdeaVersion v where v.ideaId = :ideaId")
	int maxVersionNumber(@Param("ideaId") UUID ideaId);

	List<IdeaVersion> findByIdeaIdOrderByVersionNumberDesc(UUID ideaId);

	java.util.Optional<IdeaVersion> findByIdeaIdAndVersionNumber(UUID ideaId, int versionNumber);
}
