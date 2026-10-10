package com.app.ideaforge.llm;

import java.util.List;

import jakarta.validation.Validator;

/**
 * Model-agnostic gateway. This is the only interface the rest of the app sees;
 * LangChain4j types never leak outside the llm package.
 */
public interface LlmGateway {

	/**
	 * Requests JSON output, parses it, and allows one repair retry when parsing
	 * <em>or</em> validation fails. When a validator is provided and repair still
	 * leaves only range violations (e.g. an over-long string), the output is
	 * normalized to its declared limits instead of failing the request.
	 */
	default <T> T generateJson(LlmPurpose purpose, LlmPrompt prompt, Class<T> responseType) {
		return generateJson(purpose, prompt, responseType, null);
	}

	/** Variant that validates the parsed output against Bean Validation constraints. */
	<T> T generateJson(LlmPurpose purpose, LlmPrompt prompt, Class<T> responseType, Validator validator);

	/** Free-form text generation (chat replies, markdown documents). */
	LlmTextResult generateText(LlmPurpose purpose, LlmPrompt prompt);

	/**
	 * Tool-enabled chat turn. Tools are read-only and the loop is capped at
	 * {@code maxToolCalls} executions.
	 */
	LlmToolChatResult chatWithTools(LlmPurpose purpose, LlmPrompt prompt, List<LlmTool> tools, int maxToolCalls);
}
