package com.app.ideaforge.credits;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditLedgerRepository extends JpaRepository<CreditLedgerEntry, UUID> {

	Optional<CreditLedgerEntry> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

	boolean existsByRelatedEntryId(UUID relatedEntryId);

	@org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("select l from CreditLedgerEntry l where l.id = :id")
	Optional<CreditLedgerEntry> findByIdForUpdate(@Param("id") UUID id);

	Page<CreditLedgerEntry> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

	@Query("select coalesce(sum(l.amountUnits), 0) from CreditLedgerEntry l where l.userId = :userId")
	long sumAmountsForUser(@Param("userId") UUID userId);

	/** Per-user sums for the nightly reconciliation job. */
	@Query("select l.userId, coalesce(sum(l.amountUnits), 0) from CreditLedgerEntry l group by l.userId")
	List<Object[]> sumAmountsPerUser();
}
