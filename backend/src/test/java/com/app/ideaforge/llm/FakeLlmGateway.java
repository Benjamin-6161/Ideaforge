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
	private final List<Call> calls = new CopyOnWriteArrayList<>();
	private volatile String textResponse = "ok";
	private volatile RuntimeException failWith;

	public <T> FakeLlmGateway json(Class<T> type, T response) {
		jsonResponses.put(type, response);
		return this;
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
	public <T> T generateJson(LlmPurpose purpose, LlmPrompt prompt, Class<T> responseType) {
		calls.add(new Call(purpose, prompt));
		if (failWith != null) {
			throw failWith;
		}
		Object response = jsonResponses.get(responseType);
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
		return new LlmTextResult(textResponse, new LlmUsage("fake", "fake-model", 1, 1, 1, 0));
	}
}
