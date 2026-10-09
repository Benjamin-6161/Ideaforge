package com.app.ideaforge.security;

import java.util.UUID;

/**
 * Authenticated principal stored in the security context (never an entity).
 */
public record AuthUser(UUID id, String email, String role) {

	public boolean isAdmin() {
		return "ADMIN".equals(role);
	}
}
