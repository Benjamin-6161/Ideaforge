package com.app.ideaforge.llm;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, UUID> {

	@Query("select coalesce(sum(l.estCostMicroUsd), 0) from AiUsageLog l where l.createdAt >= :since")
	long sumEstimatedCostMicroUsdSince(@Param("since") Instant since);

	/** purpose, provider, model, calls, tokens, estimated cost (for admin usage). */
	@Query("""
			select l.purpose, l.provider, l.model, count(l), sum(l.tokensIn), sum(l.tokensOut),
			       sum(l.estCostMicroUsd)
			from AiUsageLog l
			where l.createdAt >= :since
			group by l.purpose, l.provider, l.model
			order by l.purpose, l.provider, l.model
			""")
	List<Object[]> usageSince(@Param("since") Instant since);

	/** day, calls, estimated cost. */
	@Query(value = """
			select date_trunc('day', created_at) as day, count(*), coalesce(sum(est_cost_micro_usd), 0)
			from ai_usage_logs
			where created_at >= :since
			group by 1
			order by 1
			""", nativeQuery = true)
	List<Object[]> dailySpendSince(@Param("since") Instant since);
}
