package com.app.ideaforge.security;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.app.ideaforge.common.error.ApiException;
import com.app.ideaforge.common.error.ErrorCodes;

/**
 * Runs user text through the injection pre-filter and the sanitizer. Rejected
 * input never reaches credits or the LLM, and is logged as a security event.
 */
@Service
public class InputGuard {

	private final InjectionFilter injectionFilter;
	private final InputSanitizer inputSanitizer;
	private final SecurityEventService securityEvents;

	public InputGuard(
			InjectionFilter injectionFilter,
			InputSanitizer inputSanitizer,
			SecurityEventService securityEvents) {
		this.injectionFilter = injectionFilter;
		this.inputSanitizer = inputSanitizer;
		this.securityEvents = securityEvents;
	}

	public String guardSingleLine(String raw, int maxLength, UUID userId, String ipHash, String field) {
		check(raw, userId, ipHash, field);
		return inputSanitizer.sanitizeSingleLine(raw, maxLength);
	}

	public String guardText(String raw, int maxLength, UUID userId, String ipHash, String field) {
		check(raw, userId, ipHash, field);
		return inputSanitizer.sanitizeText(raw, maxLength);
	}

	private void check(String raw, UUID userId, String ipHash, String field) {
		InjectionFilter.Result result = injectionFilter.check(raw);
		if (result.blocked()) {
			securityEvents.log("PROMPT_INJECTION_BLOCKED", userId, ipHash,
					Map.of("field", field, "reason", result.reason()));
			throw ApiException.badRequest(
					ErrorCodes.INJECTION_DETECTED,
					"That input looks like an instruction attempt. Please rephrase it as a normal request.");
		}
	}
}
