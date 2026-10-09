package com.app.ideaforge.testconfig;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.app.ideaforge.llm.FakeLlmGateway;

/** Replaces the real LLM gateway with the deterministic fake in tests. */
@TestConfiguration
public class FakeLlmConfiguration {

	@Bean
	@Primary
	public FakeLlmGateway fakeLlmGateway() {
		return new FakeLlmGateway();
	}
}
