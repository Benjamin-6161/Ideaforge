package com.app.ideaforge.research;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.app.ideaforge.common.config.AppProperties;
import com.sun.net.httpserver.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

class SearxngSearchProviderTest {

	private static HttpServer server;
	private static String baseUrl;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/search", exchange -> {
			byte[] body = """
					{"query":"dental scheduling","number_of_results":2,"results":[
					  {"url":"https://example.com/a","title":"Dental scheduler","content":"Booking and reminders"},
					  {"url":"https://example.com/b","title":"Practice software","content":"RCM tools"},
					  {"url":"","title":"No URL","content":"ignored"}
					]}
					""".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.createContext("/broken", exchange -> {
			exchange.sendResponseHeaders(500, -1);
			exchange.close();
		});
		server.start();
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@AfterAll
	static void stopServer() {
		server.stop(0);
	}

	@Test
	void mapsResultsAndSkipsUrllessEntries() {
		SearxngSearchProvider provider = new SearxngSearchProvider(properties(baseUrl, true));
		var results = provider.search("dental scheduling", 5);
		assertThat(results).hasSize(2);
		assertThat(results.get(0).title()).isEqualTo("Dental scheduler");
		assertThat(results.get(0).snippet()).isEqualTo("Booking and reminders");
	}

	@Test
	void respectsMaxResults() {
		SearxngSearchProvider provider = new SearxngSearchProvider(properties(baseUrl, true));
		assertThat(provider.search("dental scheduling", 1)).hasSize(1);
	}

	@Test
	void returnsEmptyWhenDisabled() {
		SearxngSearchProvider provider = new SearxngSearchProvider(properties(baseUrl, false));
		assertThat(provider.search("anything", 5)).isEmpty();
	}

	@Test
	void failsSoftOnServerErrors() {
		SearxngSearchProvider provider = new SearxngSearchProvider(properties(baseUrl + "/broken", true));
		assertThat(provider.search("anything", 5)).isEmpty();
	}

	private AppProperties properties(String searxngBaseUrl, boolean enabled) {
		return new AppProperties(
				null, null, null, null, null, null, null, null, null,
				new AppProperties.Research(
						new AppProperties.Research.Searxng(searxngBaseUrl, enabled),
						new AppProperties.Research.HackerNews(false, "https://hn.algolia.com/api/v1"),
						new AppProperties.Research.Trends(false),
						7,
						new AppProperties.Research.PageFetcher(5, 1_048_576, 3, 8000, true)),
				null);
	}
}
