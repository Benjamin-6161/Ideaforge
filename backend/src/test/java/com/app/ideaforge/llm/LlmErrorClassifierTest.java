package com.app.ideaforge.llm;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;

import org.junit.jupiter.api.Test;

import dev.langchain4j.exception.AuthenticationException;
import dev.langchain4j.exception.InvalidRequestException;
import dev.langchain4j.exception.ModelNotFoundException;
import dev.langchain4j.exception.RateLimitException;

import static org.assertj.core.api.Assertions.assertThat;

class LlmErrorClassifierTest {

	@Test
	void classifiesProviderExceptions() {
		assertThat(LlmErrorClassifier.classify(new AuthenticationException("invalid key")))
				.isEqualTo(LlmErrorCode.UNAUTHORIZED);
		assertThat(LlmErrorClassifier.classify(new ModelNotFoundException("model 'llama3.1' not found")))
				.isEqualTo(LlmErrorCode.MODEL_NOT_FOUND);
		assertThat(LlmErrorClassifier.classify(new RateLimitException("slow down")))
				.isEqualTo(LlmErrorCode.RATE_LIMITED);
		assertThat(LlmErrorClassifier.classify(new dev.langchain4j.exception.TimeoutException("timed out")))
				.isEqualTo(LlmErrorCode.TIMEOUT);
		assertThat(LlmErrorClassifier.classify(new InvalidRequestException("bad request")))
				.isEqualTo(LlmErrorCode.INVALID_REQUEST);
	}

	@Test
	void classifiesNetworkFailures() {
		assertThat(LlmErrorClassifier.classify(new ConnectException("Connection refused")))
				.isEqualTo(LlmErrorCode.CONNECTION_FAILED);
		assertThat(LlmErrorClassifier.classify(new UnknownHostException("api.example")))
				.isEqualTo(LlmErrorCode.CONNECTION_FAILED);
		assertThat(LlmErrorClassifier.classify(new HttpTimeoutException("request timed out")))
				.isEqualTo(LlmErrorCode.TIMEOUT);
	}

	@Test
	void walksTheCauseChain() {
		RuntimeException wrapped = new RuntimeException("wrapper",
				new RuntimeException("outer", new ModelNotFoundException("model missing")));
		assertThat(LlmErrorClassifier.classify(wrapped)).isEqualTo(LlmErrorCode.MODEL_NOT_FOUND);
	}

	@Test
	void defaultsToUnknown() {
		assertThat(LlmErrorClassifier.classify(new IllegalStateException("weird")))
				.isEqualTo(LlmErrorCode.UNKNOWN);
	}

	@Test
	void runCodesAreStableAndMapped() {
		assertThat(LlmErrorCode.MODEL_NOT_FOUND.runCode()).isEqualTo("MODEL_NOT_FOUND");
		assertThat(LlmErrorCode.UNAUTHORIZED.runCode()).isEqualTo("LLM_UNAUTHORIZED");
		assertThat(LlmErrorCode.NOT_CONFIGURED.runCode()).isEqualTo("LLM_NOT_CONFIGURED");
		assertThat(LlmErrorCode.UNKNOWN.runCode()).isEqualTo("LLM_FAILED");
		assertThat(LlmErrorCode.CANARY_LEAK.runCode()).isEqualTo("CANARY_LEAK");
	}

	@Test
	void summaryUsesTheDeepestCause() {
		RuntimeException wrapped = new RuntimeException("wrapper",
				new ModelNotFoundException("model 'llama3.1' not found at http://localhost:11434"));
		String summary = LlmErrorClassifier.summary(wrapped);
		assertThat(summary).contains("ModelNotFoundException").contains("llama3.1");
	}

	@Test
	void summaryIsTrimmedAndBounded() {
		String summary = LlmErrorClassifier.summary(new IllegalStateException("x".repeat(500)));
		assertThat(summary.length()).isLessThanOrEqualTo(200);
	}

	@Test
	void summaryRedactsKeyShapedStrings() {
		String summary = LlmErrorClassifier.summary(new IllegalStateException(
				"Authentication fails, your api key: sk-abc123def456ghi is invalid"));
		assertThat(summary).contains("sk-***").doesNotContain("sk-abc123def456ghi");

		String google = LlmErrorClassifier.summary(new IllegalStateException(
				"API key not valid: AIzaSyA1234567890abcdefghijklmnopqrstuv"));
		assertThat(google).contains("AIza***").doesNotContain("AIzaSyA1234567890");
	}
}
