package com.app.ideaforge.security;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Persists security events in an isolated transaction; never fails the request. */
@Service
public class SecurityEventService {

	private static final Logger log = LoggerFactory.getLogger(SecurityEventService.class);

	private final SecurityEventRepository repository;

	public SecurityEventService(SecurityEventRepository repository) {
		this.repository = repository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void log(String type, UUID userId, String ipHash, Map<String, Object> details) {
		try {
			repository.save(new SecurityEvent(type, userId, ipHash, details));
		} catch (Exception ex) {
			log.error("Failed to persist security event {}: {}", type, ex.getMessage());
		}
	}

	public void log(String type, String ipHash) {
		log(type, null, ipHash, Map.of());
	}
}
