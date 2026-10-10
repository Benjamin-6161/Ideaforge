package com.app.ideaforge.llm;

/**
 * The model echoed the system-prompt canary token. The output is dropped and
 * callers must refund the spend.
 */
public class CanaryLeakException extends LlmException {

	public CanaryLeakException(String message) {
		super(LlmErrorCode.CANARY_LEAK, message);
	}
}
