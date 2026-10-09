package com.app.ideaforge.security;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/** Calls Cloudflare's siteverify endpoint. Fails closed on transport errors. */
public class HttpTurnstileVerifier implements TurnstileVerifier {

	private static final Logger log = LoggerFactory.getLogger(HttpTurnstileVerifier.class);

	private final RestClient restClient;
	private final String verifyUrl;
	private final String secretKey;

	public HttpTurnstileVerifier(String verifyUrl, String secretKey) {
		this.restClient = RestClient.builder().build();
		this.verifyUrl = verifyUrl;
		this.secretKey = secretKey;
	}

	@Override
	public boolean verify(String token, String remoteIp) {
		if (token == null || token.isBlank() || secretKey == null || secretKey.isBlank()) {
			return false;
		}
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("secret", secretKey);
		form.add("response", token);
		if (remoteIp != null && !remoteIp.isBlank()) {
			form.add("remoteip", remoteIp);
		}
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> response = restClient.post()
					.uri(verifyUrl)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(Map.class);
			return response != null && Boolean.TRUE.equals(response.get("success"));
		} catch (RuntimeException ex) {
			log.warn("Turnstile verification failed due to error: {}", ex.getMessage());
			return false;
		}
	}
}
