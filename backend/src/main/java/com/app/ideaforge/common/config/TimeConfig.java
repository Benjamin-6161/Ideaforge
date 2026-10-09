package com.app.ideaforge.common.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

	/** Injected everywhere so tests can fix time. */
	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
