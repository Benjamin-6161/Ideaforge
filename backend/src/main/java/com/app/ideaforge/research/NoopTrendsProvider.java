package com.app.ideaforge.research;

import java.util.List;

import org.springframework.stereotype.Component;

/** Default trends implementation: always empty, never fails a request. */
@Component
public class NoopTrendsProvider implements TrendsProvider {

	private final boolean enabled;

	public NoopTrendsProvider(com.app.ideaforge.common.config.AppProperties properties) {
		this.enabled = properties.research().trends().enabled();
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public List<TrendPoint> interest(String query, String region) {
		return List.of();
	}
}
