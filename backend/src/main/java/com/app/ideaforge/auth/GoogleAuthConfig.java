package com.app.ideaforge.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import com.app.ideaforge.common.config.AppProperties;
import com.app.ideaforge.common.error.ApiException;

@Configuration
public class GoogleAuthConfig {

	@Bean
	public GoogleTokenVerifier googleTokenVerifier(AppProperties properties) {
		String clientId = properties.auth().google().clientId();
		if (clientId == null || clientId.isBlank()) {
			return idToken -> {
				throw new ApiException(
						HttpStatus.SERVICE_UNAVAILABLE,
						"GOOGLE_NOT_CONFIGURED",
						"Google sign-in is not configured.");
			};
		}
		return new GoogleIdTokenVerifierAdapter(clientId);
	}
}
