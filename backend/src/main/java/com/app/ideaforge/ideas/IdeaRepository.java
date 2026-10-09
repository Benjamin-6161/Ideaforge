package com.app.ideaforge.ideas;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
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

	/**
	 * Filtered, sortable, paged idea list. Score-based sorts use a lateral join
	 * to the latest validation; `sort` is one of newest|demand|profitability.
	 */
	@Query(value = """
			select i.* from ideas i
			left join lateral (
			    select v.demand_score, v.profitability_score
			    from idea_validations v
			    where v.idea_id = i.id
			    order by v.created_at desc
			    limit 1
			) lv on true
			where i.user_id = :userId
			  and i.deleted_at is null
			  and i.is_archived = :archived
			  and (cast(:fieldId as uuid) is null or i.field_id = cast(:fieldId as uuid))
			  and (cast(:status as text) is null or i.status = cast(:status as text))
			  and (cast(:favorite as boolean) is null or i.is_favorite = cast(:favorite as boolean))
			  and (cast(:q as text) is null or i.title ilike '%' || cast(:q as text) || '%')
			order by
			  case when cast(:sort as text) = 'demand' then lv.demand_score end desc nulls last,
			  case when cast(:sort as text) = 'profitability' then lv.profitability_score end desc nulls last,
			  i.created_at desc
			""", countQuery = """
			select count(*) from ideas i
			where i.user_id = :userId
			  and i.deleted_at is null
			  and i.is_archived = :archived
			  and (cast(:fieldId as uuid) is null or i.field_id = cast(:fieldId as uuid))
			  and (cast(:status as text) is null or i.status = cast(:status as text))
			  and (cast(:favorite as boolean) is null or i.is_favorite = cast(:favorite as boolean))
			  and (cast(:q as text) is null or i.title ilike '%' || cast(:q as text) || '%')
			""", nativeQuery = true)
	Page<Idea> search(
			@Param("userId") UUID userId,
			@Param("fieldId") UUID fieldId,
			@Param("status") String status,
			@Param("favorite") Boolean favorite,
			@Param("archived") boolean archived,
			@Param("q") String q,
			@Param("sort") String sort,
			Pageable pageable);
}
