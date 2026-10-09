package com.app.ideaforge.security;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, UUID> {

	boolean existsByTypeAndUserId(String type, UUID userId);
}
