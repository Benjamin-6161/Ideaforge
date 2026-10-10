package com.app.ideaforge.llm;

/**
 * Classification of LLM failures so runs, logs, and the UI can name the
 * problem. These map to stable run error codes via {@link #runCode()}.
 */
public enum LlmErrorCode {
	/** Provider is unconfigured: missing key, model, base URL, or unknown provider. */
	NOT_CONFIGURED("LLM_NOT_CONFIGURED"),
	/** Provider rejected the credentials (401/403). */
	UNAUTHORIZED("LLM_UNAUTHORIZED"),
	/** The configured model does not exist at the provider (404). */
	MODEL_NOT_FOUND("MODEL_NOT_FOUND"),
	/** Provider throttled us (429). */
	RATE_LIMITED("LLM_RATE_LIMITED"),
	/** Request timed out. */
	TIMEOUT("LLM_TIMEOUT"),
	/** Network or DNS failure reaching the provider. */
	CONNECTION_FAILED("LLM_CONNECTION_FAILED"),
	/** Provider rejected the request as invalid (400/422). */
	INVALID_REQUEST("LLM_INVALID_REQUEST"),
	/** Model output failed schema validation or repair. */
	INVALID_OUTPUT("LLM_INVALID_OUTPUT"),
	/** The system canary leaked into the output. */
	CANARY_LEAK("CANARY_LEAK"),
	/** Anything else; callers must treat this as a generic provider failure. */
	UNKNOWN("LLM_FAILED");

	private final String runCode;

	LlmErrorCode(String runCode) {
		this.runCode = runCode;
	}

	public String runCode() {
		return runCode;
	}
}
