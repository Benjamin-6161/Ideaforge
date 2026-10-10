package com.app.ideaforge.llm;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;

import dev.langchain4j.exception.AuthenticationException;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.exception.InvalidRequestException;
import dev.langchain4j.exception.ModelNotFoundException;
import dev.langchain4j.exception.RateLimitException;

/**
 * Maps provider exceptions (and their causes) to stable error codes, and
 * produces a short root-cause summary for logs. Never exposes keys or bodies.
 */
public final class LlmErrorClassifier {

	private LlmErrorClassifier() {
	}

	public static LlmErrorCode classify(Throwable throwable) {
		for (Throwable candidate = throwable; candidate != null; candidate = candidate.getCause()) {
			if (candidate instanceof AuthenticationException) {
				return LlmErrorCode.UNAUTHORIZED;
			}
			if (candidate instanceof ModelNotFoundException) {
				return LlmErrorCode.MODEL_NOT_FOUND;
			}
			if (candidate instanceof RateLimitException) {
				return LlmErrorCode.RATE_LIMITED;
			}
			if (candidate instanceof dev.langchain4j.exception.TimeoutException
					|| candidate instanceof HttpTimeoutException) {
				return LlmErrorCode.TIMEOUT;
			}
			if (candidate instanceof InvalidRequestException) {
				return LlmErrorCode.INVALID_REQUEST;
			}
			if (candidate instanceof ConnectException
					|| candidate instanceof UnknownHostException
					|| candidate instanceof SocketException) {
				return LlmErrorCode.CONNECTION_FAILED;
			}
			if (candidate instanceof HttpException http) {
				int status = http.statusCode();
				if (status == 401 || status == 403) {
					return LlmErrorCode.UNAUTHORIZED;
				}
				if (status == 404) {
					return LlmErrorCode.MODEL_NOT_FOUND;
				}
				if (status == 429) {
					return LlmErrorCode.RATE_LIMITED;
				}
				if (status == 400 || status == 422) {
					return LlmErrorCode.INVALID_REQUEST;
				}
				if (status >= 500) {
					return LlmErrorCode.UNKNOWN;
				}
			}
		}
		return LlmErrorCode.UNKNOWN;
	}

	/** Deepest cause's class and message, trimmed and key-redacted for logs. */
	public static String summary(Throwable throwable) {
		Throwable root = throwable;
		while (root.getCause() != null && root.getCause() != root) {
			root = root.getCause();
		}
		String message = root.getMessage() == null ? "" : root.getMessage();
		// Some providers echo (parts of) the key back in errors; never log those.
		message = message.replaceAll("sk-[A-Za-z0-9_-]{8,}", "sk-***");
		message = message.replaceAll("AIza[0-9A-Za-z_-]{30,}", "AIza***");
		message = message.replaceAll("\\s+", " ").trim();
		String type = root.getClass().getSimpleName();
		String summary = message.isEmpty() ? type : type + ": " + message;
		if (summary.length() > 200) {
			summary = summary.substring(0, 199) + "…";
		}
		return summary;
	}
}
