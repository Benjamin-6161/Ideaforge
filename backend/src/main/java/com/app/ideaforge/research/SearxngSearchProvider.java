package com.app.ideaforge.research;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.ideaforge.common.config.AppProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * SearXNG JSON API (internal network only). Results are snippets; the provider
 * never fetches pages. Failures return an empty list.
 */
@Component
public class SearxngSearchProvider implements SearchProvider {

	private static final Logger log = LoggerFactory.getLogger(SearxngSearchProvider.class);

	private final AppProperties properties;
	private final RestClient restClient = RestClient.builder().build();

	public SearxngSearchProvider(AppProperties properties) {
		this.properties = properties;
	}

	@Override
	public List<SearchResult> search(String query, int maxResults) {
		if (!properties.research().searxng().enabled() || query == null || query.isBlank()) {
			return List.of();
		}
		try {
			String uri = UriComponentsBuilder
					.fromUriString(properties.research().searxng().baseUrl() + "/search")
					.queryParam("q", query)
					.queryParam("format", "json")
					.queryParam("language", "en")
					.queryParam("safesearch", 0)
					.build()
					.encode()
					.toUriString();
			SearxResponse response = restClient.get()
					.uri(uri)
					.retrieve()
					.body(SearxResponse.class);
			if (response == null || response.results() == null) {
				return List.of();
			}
			return response.results().stream()
					.filter(result -> result.url() != null && !result.url().isBlank())
					.limit(Math.max(1, maxResults))
					.map(result -> new SearchResult(
							result.title() == null ? "" : result.title(),
							result.url(),
							result.content() == null ? "" : result.content()))
					.toList();
		} catch (Exception ex) {
			log.debug("SearXNG search failed for '{}': {}", query, ex.getMessage());
			return List.of();
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record SearxResponse(List<SearxResult> results) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record SearxResult(String url, String title, String content) {
	}
}
