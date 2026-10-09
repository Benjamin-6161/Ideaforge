package com.app.ideaforge.llm;

/** Purpose of an LLM call; also the category used in ai_usage_logs. */
public enum LlmPurpose {
	IDEATION,
	SCORING,
	CHAT,
	DEEP_VALIDATION,
	FINALIZE
}
