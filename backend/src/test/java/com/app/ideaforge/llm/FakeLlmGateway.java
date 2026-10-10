package com.app.ideaforge.llm;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Deterministic gateway for tests. Register JSON responses per type and
 * optional text replies; all calls are recorded.
 */
public class FakeLlmGateway implements LlmGateway {

	public record Call(LlmPurpose purpose, LlmPrompt prompt) {
	}

	private final Map<Class<?>, Object> jsonResponses = new ConcurrentHashMap<>();
	private final Map<Class<?>, java.util.Queue<Object>> jsonSequences = new ConcurrentHashMap<>();
	private final Map<Class<?>, java.util.Queue<Object>> textSequences = new ConcurrentHashMap<>();
	private final List<Call> calls = new CopyOnWriteArrayList<>();
	private volatile String textResponse = "ok";
	private volatile RuntimeException failWith;

	public <T> FakeLlmGateway json(Class<T> type, T response) {
		jsonResponses.put(type, response);
		return this;
	}

	public FakeLlmGateway textSequence(String... responses) {
		java.util.Queue<Object> queue = textSequences.computeIfAbsent(
				String.class, key -> new java.util.concurrent.ConcurrentLinkedQueue<>());
		for (String response : responses) {
			queue.add(response);
		}
		return this;
	}

	@SafeVarargs
	public final <T> FakeLlmGateway jsonSequence(Class<T> type, T... responses) {
		java.util.Queue<Object> queue = jsonSequences.computeIfAbsent(type, key -> new java.util.concurrent.ConcurrentLinkedQueue<>());
		for (T response : responses) {
			queue.add(response);
		}
		return this;
	}

	public void reset() {
		jsonResponses.clear();
		jsonSequences.clear();
		textSequences.clear();
		calls.clear();
		textResponse = "ok";
		failWith = null;
	}

	public FakeLlmGateway text(String response) {
		this.textResponse = response;
		return this;
	}

	public FakeLlmGateway failWith(RuntimeException exception) {
		this.failWith = exception;
		return this;
	}

	public List<Call> calls() {
		return List.copyOf(calls);
	}

	public Call lastCall() {
		return calls.get(calls.size() - 1);
	}

	@Override
	public <T> T generateJson(
			LlmPurpose purpose,
			LlmPrompt prompt,
			Class<T> responseType,
			jakarta.validation.Validator validator) {
		calls.add(new Call(purpose, prompt));
		if (failWith != null) {
			throw failWith;
		}
		java.util.Queue<Object> sequence = jsonSequences.get(responseType);
		Object response = sequence == null ? null : sequence.poll();
		if (response == null) {
			response = jsonResponses.get(responseType);
		}
		if (response == null) {
			throw new LlmException("FakeLlmGateway has no JSON response registered for " + responseType.getSimpleName());
		}
		return responseType.cast(response);
	}

	@Override
	public LlmTextResult generateText(LlmPurpose purpose, LlmPrompt prompt) {
		calls.add(new Call(purpose, prompt));
		if (failWith != null) {
			throw failWith;
		}
		java.util.Queue<Object> sequence = textSequences.get(String.class);
		Object response = sequence == null ? null : sequence.poll();
		String text = response == null ? textResponse : (String) response;
		return new LlmTextResult(text, new LlmUsage("fake", "fake-model", 1, 1, 1, 0));
	}

	@Override
	public LlmToolChatResult chatWithTools(
			LlmPurpose purpose,
			LlmPrompt prompt,
			java.util.List<LlmTool> tools,
			int maxToolCalls) {
		calls.add(new Call(purpose, prompt));
		if (failWith != null) {
			throw failWith;
		}
		return new LlmToolChatResult(textResponse, java.util.List.of(), new LlmUsage("fake", "fake-model", 1, 1, 1, 0));
	}
}
