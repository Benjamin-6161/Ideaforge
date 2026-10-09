package com.app.ideaforge.research;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.ideaforge.common.config.AppProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Hacker News (Algolia) search: free, no key, useful as a demand/competition
 * signal for technical products. Failures return an empty list.
 */
@Component
public class HackerNewsProvider implements CommunitySignalProvider {

	private static final Logger log = LoggerFactory.getLogger(HackerNewsProvider.class);

	private final AppProperties properties;
	private final RestClient restClient = RestClient.builder().build();

	public HackerNewsProvider(AppProperties properties) {
		this.properties = properties;
	}

	@Override
	public List<CommunitySignal> search(String query, int maxResults) {
		if (!properties.research().hackerNews().enabled() || query == null || query.isBlank()) {
			return List.of();
		}
		try {
			String uri = UriComponentsBuilder
					.fromUriString(properties.research().hackerNews().baseUrl() + "/search")
					.queryParam("query", query)
					.queryParam("tags", "story")
					.queryParam("hitsPerPage", Math.max(1, maxResults))
					.build()
					.encode()
					.toUriString();
			HnResponse response = restClient.get()
					.uri(uri)
					.retrieve()
					.body(HnResponse.class);
			if (response == null || response.hits() == null) {
				return List.of();
			}
			return response.hits().stream()
					.map(hit -> new CommunitySignal(
							hit.title() == null ? "" : hit.title(),
							hit.url() == null || hit.url().isBlank()
									? "https://news.ycombinator.com/item?id=" + hit.objectId()
									: hit.url(),
							hit.points() == null ? 0 : hit.points(),
							hit.numComments() == null ? 0 : hit.numComments(),
							parseCreatedAt(hit.createdAt())))
					.toList();
		} catch (Exception ex) {
			log.debug("Hacker News search failed for '{}': {}", query, ex.getMessage());
			return List.of();
		}
	}

	private Instant parseCreatedAt(String value) {
		try {
			return value == null ? null : Instant.parse(value);
		} catch (Exception ex) {
			return null;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record HnResponse(List<HnHit> hits) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record HnHit(
			String title,
			String url,
			Integer points,
			@JsonProperty("num_comments") Integer numComments,
			@JsonProperty("created_at") String createdAt,
			@JsonProperty("objectID") String objectId) {
	}
}
