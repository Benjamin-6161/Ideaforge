package com.app.ideaforge.llm;

/**
 * Model-agnostic gateway. This is the only interface the rest of the app sees;
 * LangChain4j types never leak outside the llm package.
 */
public interface LlmGateway {

	/**
	 * Requests JSON output, parses it into the given type, and allows at most
	 * one repair retry when parsing fails.
	 */
	<T> T generateJson(LlmPurpose purpose, LlmPrompt prompt, Class<T> responseType);

	/** Free-form text generation (chat replies, markdown documents). */
	LlmTextResult generateText(LlmPurpose purpose, LlmPrompt prompt);
}
