package com.app.ideaforge.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {

	Optional<UserIdentity> findByProviderAndProviderSubject(IdentityProvider provider, String providerSubject);

	boolean existsByUserIdAndProvider(UUID userId, IdentityProvider provider);
}
