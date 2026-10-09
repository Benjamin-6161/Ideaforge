package com.app.ideaforge.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

	Optional<OtpChallenge> findTopByEmailNormalizedOrderByCreatedAtDesc(String emailNormalized);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from OtpChallenge c where c.emailNormalized = :email and c.consumedAt is null order by c.createdAt desc")
	Optional<OtpChallenge> findActiveForUpdate(@Param("email") String emailNormalized);

	/** Invalidate all outstanding challenges for an email (resend or max attempts). */
	@Modifying
	@Query("update OtpChallenge c set c.consumedAt = :now where c.emailNormalized = :email and c.consumedAt is null")
	int consumeAllActive(@Param("email") String emailNormalized, @Param("now") Instant now);
}
