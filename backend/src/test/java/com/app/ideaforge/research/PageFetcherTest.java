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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageFetcherTest {

	private static HttpServer server;
	private static String baseUrl;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/article", exchange -> respond(exchange,
				"text/html; charset=utf-8",
				"<html><head><title>Dental scheduling</title></head>"
						+ "<body><h1>No-shows</h1><p>Clinics lose revenue to missed appointments every week.</p></body></html>"));
		server.createContext("/redirect", exchange -> {
			exchange.getResponseHeaders().add("Location", "/article");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		server.createContext("/r1", exchange -> {
			exchange.getResponseHeaders().add("Location", "/r2");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		server.createContext("/r2", exchange -> {
			exchange.getResponseHeaders().add("Location", "/r3");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		server.createContext("/r3", exchange -> {
			exchange.getResponseHeaders().add("Location", "/article");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		server.createContext("/long", exchange -> respond(exchange, "text/html", "<html><body>"
				+ "long ".repeat(500) + "</body></html>"));
		server.createContext("/binary", exchange -> respond(exchange, "application/pdf", "%PDF-1.7"));
		server.createContext("/error", exchange -> {
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

	private static void respond(com.sun.net.httpserver.HttpExchange exchange, String contentType, String body)
			throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", contentType);
		exchange.sendResponseHeaders(200, bytes.length);
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(bytes);
		}
	}

	@Test
	void extractsTitleAndText() {
		PageFetcher fetcher = fetcher(true, 3, 8000, true);
		FetchedPage page = fetcher.fetch(baseUrl + "/article");
		assertThat(page.title()).isEqualTo("Dental scheduling");
		assertThat(page.text()).contains("missed appointments");
	}

	@Test
	void followsRedirects() {
		PageFetcher fetcher = fetcher(true, 3, 8000, true);
		FetchedPage page = fetcher.fetch(baseUrl + "/redirect");
		assertThat(page.url()).isEqualTo(baseUrl + "/article");
	}

	@Test
	void rejectsTooManyRedirects() {
		PageFetcher fetcher = fetcher(true, 1, 8000, true);
		assertThatThrownBy(() -> fetcher.fetch(baseUrl + "/r1"))
				.isInstanceOf(ResearchException.class)
				.hasMessageContaining("redirect");
	}

	@Test
	void truncatesLongPagesToMaxChars() {
		PageFetcher fetcher = fetcher(true, 3, 100, true);
		FetchedPage page = fetcher.fetch(baseUrl + "/long");
		assertThat(page.text().length()).isLessThanOrEqualTo(100);
		assertThat(page.truncated()).isTrue();
	}

	@Test
	void rejectsBinaryContentTypes() {
		PageFetcher fetcher = fetcher(true, 3, 8000, true);
		assertThatThrownBy(() -> fetcher.fetch(baseUrl + "/binary"))
				.isInstanceOf(ResearchException.class)
				.hasMessageContaining("content type");
	}

	@Test
	void rejectsNonSuccessStatuses() {
		PageFetcher fetcher = fetcher(true, 3, 8000, true);
		assertThatThrownBy(() -> fetcher.fetch(baseUrl + "/error"))
				.isInstanceOf(ResearchException.class)
				.hasMessageContaining("status");
	}

	@Test
	void rejectsPrivateHostsWhenNotPermitted() {
		PageFetcher fetcher = fetcher(false, 3, 8000, false);
		assertThatThrownBy(() -> fetcher.fetch(baseUrl + "/article"))
				.isInstanceOf(ResearchException.class)
				.hasMessageContaining("not allowed");
	}

	@Test
	void rejectsNonHttpSchemes() {
		PageFetcher fetcher = fetcher(true, 3, 8000, true);
		assertThatThrownBy(() -> fetcher.fetch("file:///etc/passwd"))
				.isInstanceOf(ResearchException.class);
	}

	private PageFetcher fetcher(boolean allowPrivate, int maxRedirects, int maxChars, boolean mx) {
		AppProperties properties = new AppProperties(
				null, null, null, null, null, null, null, null, null,
				new AppProperties.Research(
						new AppProperties.Research.Searxng("http://localhost:8088", false),
						new AppProperties.Research.HackerNews(false, "https://hn.algolia.com/api/v1"),
						new AppProperties.Research.Trends(false),
						7,
						new AppProperties.Research.PageFetcher(5, 1_048_576, maxRedirects, maxChars, allowPrivate)),
				null);
		return new PageFetcher(properties, new SsrfGuard(properties));
	}
}
