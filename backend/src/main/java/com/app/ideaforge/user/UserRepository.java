package com.app.ideaforge.user;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmailNormalized(String emailNormalized);

	long countBySignupIpHashAndCreatedAtAfter(String signupIpHash, Instant after);
}
