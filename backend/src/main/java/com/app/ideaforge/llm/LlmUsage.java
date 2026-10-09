package com.app.ideaforge.llm;

/** Provider/model attribution and estimated cost of an LLM call. */
public record LlmUsage(
		String provider,
		String model,
		int inputTokens,
		int outputTokens,
		long latencyMs,
		long estimatedCostMicroUsd) {
}
