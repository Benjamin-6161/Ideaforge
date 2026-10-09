package com.app.ideaforge.auth;

/** Verifies Google Identity Services ID tokens. Interface so tests can fake it. */
public interface GoogleTokenVerifier {

	GoogleAccount verify(String idToken);

	record GoogleAccount(
			String subject,
			String email,
			boolean emailVerified,
			String name,
			String picture) {
	}
}
