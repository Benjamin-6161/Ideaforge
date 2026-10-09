package com.app.ideaforge.research;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Cached provider results, keyed by hash of provider + normalized query. */
@Entity
@Table(name = "search_cache")
public class SearchCacheEntry {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(nullable = false)
	private String provider;

	@Column(name = "query_hash", nullable = false, unique = true)
	private String queryHash;

	@Column(nullable = false)
	private String query;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb", nullable = false)
	private List<SearchResult> results = List.of();

	@Column(name = "fetched_at", nullable = false)
	private Instant fetchedAt = Instant.now();

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	protected SearchCacheEntry() {
	}

	public SearchCacheEntry(
			String provider,
			String queryHash,
			String query,
			List<SearchResult> results,
			Instant fetchedAt,
			Instant expiresAt) {
		this.provider = provider;
		this.queryHash = queryHash;
		this.query = query;
		this.results = results;
		this.fetchedAt = fetchedAt;
		this.expiresAt = expiresAt;
	}

	public void refresh(List<SearchResult> results, Instant fetchedAt, Instant expiresAt) {
		this.results = results;
		this.fetchedAt = fetchedAt;
		this.expiresAt = expiresAt;
	}

	public UUID getId() {
		return id;
	}

	public String getProvider() {
		return provider;
	}

	public String getQueryHash() {
		return queryHash;
	}

	public String getQuery() {
		return query;
	}

	public List<SearchResult> getResults() {
		return results;
	}

	public Instant getFetchedAt() {
		return fetchedAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}
}
