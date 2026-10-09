package com.app.ideaforge.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

	Optional<AuthSession> findByTokenHash(String tokenHash);

	@Modifying
	@Query("update AuthSession s set s.revokedAt = :now where s.userId = :userId and s.revokedAt is null")
	int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
