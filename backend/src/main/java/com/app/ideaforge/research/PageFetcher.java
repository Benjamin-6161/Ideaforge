package com.app.ideaforge.research;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import com.app.ideaforge.common.config.AppProperties;

/**
 * SSRF-safe page fetcher: http/https only, every hop re-validated, max
 * redirects, timeout, byte cap, text extraction only.
 */
@Component
public class PageFetcher {

	private final AppProperties properties;
	private final SsrfGuard ssrfGuard;
	private final HttpClient httpClient;

	public PageFetcher(AppProperties properties, SsrfGuard ssrfGuard) {
		this.properties = properties;
		this.ssrfGuard = ssrfGuard;
		this.httpClient = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NEVER)
				.connectTimeout(Duration.ofSeconds(properties.research().pageFetcher().timeoutSeconds()))
				.build();
	}

	public FetchedPage fetch(String url) {
		URI current = parse(url);
		int redirects = 0;
		int maxRedirects = properties.research().pageFetcher().maxRedirects();

		while (true) {
			if (!ssrfGuard.isAllowed(current)) {
				throw new ResearchException("URL is not allowed: " + current);
			}
			HttpRequest request = HttpRequest.newBuilder(current)
					.timeout(Duration.ofSeconds(properties.research().pageFetcher().timeoutSeconds()))
					.header("User-Agent", "IdeaForgeBot/1.0 (+https://ideaforge.local; research)")
					.header("Accept", "text/html,application/xhtml+xml,text/plain")
					.GET()
					.build();
			HttpResponse<InputStream> response;
			try {
				response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
			} catch (IOException ex) {
				throw new ResearchException("Could not fetch URL", ex);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new ResearchException("Fetch interrupted", ex);
			}

			int status = response.statusCode();
			if (status >= 300 && status < 400) {
				closeQuietly(response.body());
				if (redirects >= maxRedirects) {
					throw new ResearchException("Too many redirects");
				}
				String location = response.headers().firstValue("location").orElse(null);
				if (location == null || location.isBlank()) {
					throw new ResearchException("Redirect without a location header");
				}
				current = current.resolve(location);
				redirects++;
				continue;
			}
			if (status != 200) {
				closeQuietly(response.body());
				throw new ResearchException("Unexpected status " + status);
			}
			String contentType = response.headers().firstValue("content-type").orElse("");
			if (!isTextContentType(contentType)) {
				closeQuietly(response.body());
				throw new ResearchException("Unsupported content type: " + contentType);
			}
			return extract(current, response);
		}
	}

	private FetchedPage extract(URI uri, HttpResponse<InputStream> response) {
		long maxBytes = properties.research().pageFetcher().maxBytes();
		int maxChars = properties.research().pageFetcher().maxChars();
		byte[] body;
		try (InputStream input = response.body()) {
			body = readCapped(input, maxBytes);
		} catch (IOException ex) {
			throw new ResearchException("Failed to read response body", ex);
		}
		boolean truncated = body.length > maxBytes;
		String html = new String(body, charsetOf(response));
		Document document = Jsoup.parse(html, uri.toString());
		String title = document.title() == null ? "" : document.title().trim();
		String text = document.body() == null ? document.text() : document.body().text();
		if (text.length() > maxChars) {
			text = text.substring(0, maxChars);
			truncated = true;
		}
		return new FetchedPage(uri.toString(), title, text, truncated);
	}

	private byte[] readCapped(InputStream input, long maxBytes) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		long total = 0;
		int read;
		while (total <= maxBytes && (read = input.read(buffer)) != -1) {
			int allowed = (int) Math.min(read, maxBytes + 1 - total);
			output.write(buffer, 0, Math.max(0, allowed));
			total += allowed;
		}
		return output.toByteArray();
	}

	private java.nio.charset.Charset charsetOf(HttpResponse<InputStream> response) {
		String contentType = response.headers().firstValue("content-type").orElse("");
		int index = contentType.toLowerCase().indexOf("charset=");
		if (index >= 0) {
			String charset = contentType.substring(index + "charset=".length()).split(";")[0].trim().replace("\"", "");
			try {
				return java.nio.charset.Charset.forName(charset);
			} catch (Exception ignored) {
				// fall through to UTF-8
			}
		}
		return StandardCharsets.UTF_8;
	}

	private boolean isTextContentType(String contentType) {
		String lowered = contentType.toLowerCase();
		return lowered.contains("text/html")
				|| lowered.contains("application/xhtml")
				|| lowered.contains("text/plain");
	}

	private URI parse(String url) {
		try {
			return URI.create(url);
		} catch (IllegalArgumentException ex) {
			throw new ResearchException("Invalid URL", ex);
		}
	}

	private void closeQuietly(InputStream input) {
		try {
			input.close();
		} catch (IOException ignored) {
			// nothing to do
		}
	}
}
