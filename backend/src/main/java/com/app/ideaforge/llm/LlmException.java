package com.app.ideaforge.llm;

/** Provider or parsing failure with a classification and no leaked internals. */
public class LlmException extends RuntimeException {

	private final LlmErrorCode code;

	public LlmException(String message) {
		this(LlmErrorCode.UNKNOWN, message, null);
	}

	public LlmException(String message, Throwable cause) {
		this(LlmErrorCode.UNKNOWN, message, cause);
	}

	public LlmException(LlmErrorCode code, String message) {
		this(code, message, null);
	}

	public LlmException(LlmErrorCode code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	public LlmErrorCode getCode() {
		return code;
	}
}
