package com.app.ideaforge.research;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.app.ideaforge.common.config.AppProperties;
import com.app.ideaforge.generation.TestIdeas;
import com.sun.net.httpserver.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression: fetching competitor pages must not mutate the evidence list
 * while iterating it.
 */
class DeepResearchServiceTest {

	private static HttpServer server;
	private static String pageUrl;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/competitor", exchange -> {
			byte[] body = ("<html><head><title>Scheduler Pro</title></head><body>"
					+ "Scheduling software for clinics. Pricing starts at $99 per location per month."
					+ "</body></html>").getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/html");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.start();
		pageUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/competitor";
	}

	@AfterAll
	static void stopServer() {
		server.stop(0);
	}

	@Test
	void fetchesCompetitorPagesAndAddsPageEvidence() {
		SearchProvider searchProvider = mock(SearchProvider.class);
		when(searchProvider.search(anyString(), anyInt())).thenAnswer(invocation -> List.of(
				new SearchResult("Scheduler Pro", pageUrl, "Scheduling software"),
				new SearchResult("Other", "http://127.0.0.1:1/unreachable", "snippet only")));
		CommunitySignalProvider community = mock(CommunitySignalProvider.class);
		when(community.search(anyString(), anyInt())).thenReturn(List.of());

		AppProperties properties = properties();
		PageFetcher pageFetcher = new PageFetcher(properties, new SsrfGuard(properties));
		assertThat(pageFetcher.fetch(pageUrl).title()).isEqualTo("Scheduler Pro");
		DeepResearchService service = new DeepResearchService(
				searchProvider,
				community,
				new SearchCacheService(
						mock(SearchCacheRepository.class),
						new com.app.ideaforge.security.HashingService(),
						properties,
						java.time.Clock.systemUTC()) {
					@Override
					public List<SearchResult> getOrLoad(String provider, String query,
							java.util.function.Supplier<List<SearchResult>> loader) {
						return loader.get();
					}
				},
				pageFetcher,
				new NoopTrendsProvider(properties));

		var bundle = service.research(TestIdeas.idea("Dental reminders"));

		assertThat(bundle.evidence()).anyMatch(item -> "page".equals(item.kind()));
		assertThat(bundle.fetchedCompetitors()).hasSize(1);
		assertThat(bundle.fetchedCompetitors().get(0).name()).isEqualTo("Scheduler Pro");
		assertThat(bundle.queries()).hasSize(4);
	}

	private AppProperties properties() {
		return new AppProperties(
				null, null, null, null, null, null, null, null, null,
				new AppProperties.Research(
						new AppProperties.Research.Searxng("http://localhost:8088", false),
						new AppProperties.Research.HackerNews(false, "https://hn.algolia.com/api/v1"),
						new AppProperties.Research.Trends(false),
						7,
						new AppProperties.Research.PageFetcher(5, 1_048_576, 3, 8000, true)),
				null);
	}
}
