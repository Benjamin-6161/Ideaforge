package com.app.ideaforge.llm;

/**
 * A fully composed prompt: rules in the system message, untrusted content in
 * clearly delimited blocks in the user message.
 */
public record LlmPrompt(String system, String user) {
}
