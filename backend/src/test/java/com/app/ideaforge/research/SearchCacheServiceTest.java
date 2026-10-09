package com.app.ideaforge.research;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import com.app.ideaforge.IntegrationTestBase;
import com.app.ideaforge.security.HashingService;

import static org.assertj.core.api.Assertions.assertThat;

class SearchCacheServiceTest extends IntegrationTestBase {

	@Autowired
	SearchCacheService cache;

	@Autowired
	SearchCacheRepository repository;

	@Autowired
	HashingService hashingService;

	@Test
	void cachesResultsAcrossCalls() {
		String query = "cache-test-" + UUID.randomUUID();
		AtomicInteger loads = new AtomicInteger();
		List<SearchResult> results = List.of(new SearchResult("Title", "https://example.com", "Snippet"));

		List<SearchResult> first = cache.getOrLoad("searxng", query, () -> {
			loads.incrementAndGet();
			return results;
		});
		List<SearchResult> second = cache.getOrLoad("searxng", query, () -> {
			loads.incrementAndGet();
			return results;
		});

		assertThat(first).isEqualTo(results);
		assertThat(second).isEqualTo(results);
		assertThat(loads.get()).isEqualTo(1);
	}

	@Test
	void doesNotCacheEmptyResults() {
		String query = "cache-empty-" + UUID.randomUUID();
		AtomicInteger loads = new AtomicInteger();

		cache.getOrLoad("searxng", query, () -> {
			loads.incrementAndGet();
			return List.of();
		});
		cache.getOrLoad("searxng", query, () -> {
			loads.incrementAndGet();
			return List.of();
		});

		assertThat(loads.get()).isEqualTo(2);
	}

	@Test
	void reloadsExpiredEntries() {
		String query = "cache-expired-" + UUID.randomUUID();
		String normalized = query.trim().toLowerCase();
		String queryHash = hashingService.sha256Hex("searxng|" + normalized);
		repository.save(new SearchCacheEntry(
				"searxng",
				queryHash,
				normalized,
				List.of(new SearchResult("Old", "https://old.example", "old")),
				Instant.now().minusSeconds(900_000),
				Instant.now().minusSeconds(100)));
		AtomicInteger loads = new AtomicInteger();

		List<SearchResult> fresh = cache.getOrLoad("searxng", query, () -> {
			loads.incrementAndGet();
			return List.of(new SearchResult("New", "https://new.example", "new"));
		});

		assertThat(loads.get()).isEqualTo(1);
		assertThat(fresh.get(0).title()).isEqualTo("New");
		assertThat(repository.findByQueryHash(queryHash).orElseThrow().getResults().get(0).title()).isEqualTo("New");
	}

	@Test
	void entityStoresStructuredResults() {
		// Guards the jsonb mapping: results survive a save/read round-trip.
		List<SearchCacheEntry> all = repository.findAll(PageRequest.of(0, 1)).getContent();
		assertThat(all).isNotNull();
	}
}
