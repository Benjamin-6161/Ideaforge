package com.app.ideaforge.research;

import java.net.URI;

import org.junit.jupiter.api.Test;

import com.app.ideaforge.common.config.AppProperties;

import static org.assertj.core.api.Assertions.assertThat;

class SsrfGuardTest {

	private final SsrfGuard guard = new SsrfGuard(properties(false));
	private final SsrfGuard permissiveGuard = new SsrfGuard(properties(true));

	@Test
	void blocksLoopbackAndPrivateRanges() {
		assertThat(guard.isAllowed(URI.create("http://127.0.0.1/"))).isFalse();
		assertThat(guard.isAllowed(URI.create("http://localhost/"))).isFalse();
		assertThat(guard.isAllowed(URI.create("http://[::1]/"))).isFalse();
		assertThat(guard.isAllowed(URI.create("http://10.0.0.5/"))).isFalse();
		assertThat(guard.isAllowed(URI.create("http://172.16.1.1/"))).isFalse();
		assertThat(guard.isAllowed(URI.create("http://192.168.1.1/"))).isFalse();
		assertThat(guard.isAllowed(URI.create("http://169.254.169.254/latest/meta-data/"))).isFalse();
		assertThat(guard.isAllowed(URI.create("http://100.64.0.1/"))).isFalse();
		assertThat(guard.isAllowed(URI.create("http://0.0.0.0/"))).isFalse();
		assertThat(guard.isAllowed(URI.create("http://224.0.0.1/"))).isFalse();
		assertThat(guard.isAllowed(URI.create("http://198.18.0.1/"))).isFalse();
		assertThat(guard.isAllowed(URI.create("http://192.0.2.10/"))).isFalse();
		assertThat(guard.isAllowed(URI.create("http://203.0.113.7/"))).isFalse();
	}

	@Test
	void blocksNonHttpSchemes() {
		assertThat(guard.isAllowed(URI.create("file:///etc/passwd"))).isFalse();
		assertThat(guard.isAllowed(URI.create("ftp://example.com/file"))).isFalse();
		assertThat(guard.isAllowed(URI.create("gopher://example.com/"))).isFalse();
		assertThat(guard.isAllowed(URI.create("javascript:alert(1)"))).isFalse();
	}

	@Test
	void allowsPublicAddresses() {
		assertThat(guard.isAllowed(URI.create("http://93.184.216.34/"))).isTrue();
		assertThat(guard.isAllowed(URI.create("https://1.1.1.1/"))).isTrue();
		assertThat(guard.isAllowed(URI.create("https://8.8.8.8/"))).isTrue();
		assertThat(guard.isAllowed(URI.create("https://[2606:4700:4700::1111]/"))).isTrue();
	}

	@Test
	void permissiveFlagAllowsPrivateHostsForTestsOnly() {
		assertThat(permissiveGuard.isAllowed(URI.create("http://127.0.0.1:8080/"))).isTrue();
	}

	private AppProperties properties(boolean allowPrivate) {
		return new AppProperties(
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				new AppProperties.Research(
						new AppProperties.Research.Searxng("http://localhost:8088", false),
						new AppProperties.Research.HackerNews(false, "https://hn.algolia.com/api/v1"),
						new AppProperties.Research.Trends(false),
						7,
						new AppProperties.Research.PageFetcher(5, 1_048_576, 3, 8000, allowPrivate)),
				null);
	}
}
