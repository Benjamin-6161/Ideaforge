package com.app.ideaforge.llm;

import java.util.List;

/**
 * A read-only capability the chat agent may call. The chat package implements
 * these; the gateway translates them for the provider. No tool can touch the
 * database, other users, or credits.
 */
public interface LlmTool {

	String name();

	String description();

	List<ToolParameter> parameters();

	/**
	 * Executes with JSON-encoded arguments; must return a compact string
	 * (JSON or plain text) that will be fed back to the model.
	 */
	String execute(String jsonArguments);

	record ToolParameter(String name, String type, String description, boolean required) {
	}
}
