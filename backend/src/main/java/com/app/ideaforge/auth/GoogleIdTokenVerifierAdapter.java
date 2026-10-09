package com.app.ideaforge.auth;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.app.ideaforge.common.error.ApiException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

/** Google ID token verification with audience and email_verified checks. */
public class GoogleIdTokenVerifierAdapter implements GoogleTokenVerifier {

	private static final Logger log = LoggerFactory.getLogger(GoogleIdTokenVerifierAdapter.class);

	private final GoogleIdTokenVerifier verifier;

	public GoogleIdTokenVerifierAdapter(String clientId) {
		this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
				.setAudience(List.of(clientId))
				.build();
	}

	@Override
	public GoogleAccount verify(String idToken) {
		try {
			GoogleIdToken token = verifier.verify(idToken);
			if (token == null) {
				throw invalidToken();
			}
			GoogleIdToken.Payload payload = token.getPayload();
			if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
				throw invalidToken();
			}
			String email = payload.getEmail();
			if (email == null || email.isBlank()) {
				throw invalidToken();
			}
			return new GoogleAccount(
					payload.getSubject(),
					email,
					true,
					payload.get("name") instanceof String name ? name : null,
					payload.get("picture") instanceof String picture ? picture : null);
		} catch (ApiException ex) {
			throw ex;
		} catch (Exception ex) {
			log.warn("Google ID token verification error: {}", ex.getMessage());
			throw invalidToken();
		}
	}

	private ApiException invalidToken() {
		return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Google sign-in failed.");
	}
}
