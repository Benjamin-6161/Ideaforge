package com.app.ideaforge.llm;

import java.util.List;

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

	/**
	 * Tool-enabled chat turn. Tools are read-only and the loop is capped at
	 * {@code maxToolCalls} executions.
	 */
	LlmToolChatResult chatWithTools(LlmPurpose purpose, LlmPrompt prompt, List<LlmTool> tools, int maxToolCalls);
}
