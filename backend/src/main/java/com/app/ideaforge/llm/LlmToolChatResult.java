package com.app.ideaforge.llm;

import java.util.List;

/** Result of a tool-enabled chat turn. */
public record LlmToolChatResult(String text, List<String> toolInvocations, LlmUsage usage) {
}
