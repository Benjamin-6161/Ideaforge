package com.app.ideaforge.credits;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditWalletRepository extends JpaRepository<CreditWallet, UUID> {

	/** Atomic spend guard: only succeeds when the balance is sufficient. */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update CreditWallet w
			set w.balanceUnits = w.balanceUnits - :cost, w.updatedAt = :now
			where w.userId = :userId and w.balanceUnits >= :cost
			""")
	int debit(@Param("userId") UUID userId, @Param("cost") long cost, @Param("now") Instant now);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update CreditWallet w
			set w.balanceUnits = w.balanceUnits + :amount, w.updatedAt = :now
			where w.userId = :userId
			""")
	int credit(@Param("userId") UUID userId, @Param("amount") long amount, @Param("now") Instant now);

	/** Serializes concurrent spends for one user; refunds lock the spend row instead. */
	@Query(value = "select * from credit_wallets where user_id = :userId for update", nativeQuery = true)
	Optional<CreditWallet> lockByUserId(@Param("userId") UUID userId);

	@Modifying
	@Query(value = """
			insert into credit_wallets (user_id, balance_units, updated_at)
			values (:userId, 0, :now)
			on conflict (user_id) do nothing
			""", nativeQuery = true)
	int ensureExists(@Param("userId") UUID userId, @Param("now") Instant now);

	@Query("select w.balanceUnits from CreditWallet w where w.userId = :userId")
	Long findBalance(@Param("userId") UUID userId);
}
