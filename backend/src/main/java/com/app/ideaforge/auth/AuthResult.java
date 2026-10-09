package com.app.ideaforge.auth;

import java.time.Instant;

import com.app.ideaforge.user.User;

/** Result of a successful login: the user plus the new session token. */
public record AuthResult(User user, String token, Instant expiresAt) {
}
