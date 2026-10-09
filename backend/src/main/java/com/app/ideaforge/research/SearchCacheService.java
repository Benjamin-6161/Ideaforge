package com.app.ideaforge.research;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.ideaforge.common.config.AppProperties;
import com.app.ideaforge.security.HashingService;

/**
 * Seven-day search cache keyed by provider + normalized query. Empty results
 * are not cached so transient provider failures recover quickly.
 */
@Service
public class SearchCacheService {

	private final SearchCacheRepository repository;
	private final HashingService hashingService;
	private final AppProperties properties;
	private final Clock clock;

	public SearchCacheService(
			SearchCacheRepository repository,
			HashingService hashingService,
			AppProperties properties,
			Clock clock) {
		this.repository = repository;
		this.hashingService = hashingService;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	public List<SearchResult> getOrLoad(String provider, String query, Supplier<List<SearchResult>> loader) {
		String normalized = normalize(query);
		String queryHash = hashingService.sha256Hex(provider + "|" + normalized);
		Instant now = clock.instant();

		var existing = repository.findByQueryHash(queryHash);
		if (existing.isPresent() && existing.get().getExpiresAt().isAfter(now)) {
			return existing.get().getResults();
		}

		List<SearchResult> loaded = loader.get();
		if (loaded == null || loaded.isEmpty()) {
			return List.of();
		}
		Instant expiresAt = now.plus(Duration.ofDays(properties.research().searchCacheDays()));
		if (existing.isPresent()) {
			existing.get().refresh(loaded, now, expiresAt);
			repository.save(existing.get());
		} else {
			repository.save(new SearchCacheEntry(provider, queryHash, normalized, loaded, now, expiresAt));
		}
		return loaded;
	}

	private String normalize(String query) {
		return query == null ? "" : query.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
	}
}
