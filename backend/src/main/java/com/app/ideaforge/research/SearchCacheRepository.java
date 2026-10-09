package com.app.ideaforge.research;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SearchCacheRepository extends JpaRepository<SearchCacheEntry, UUID> {

	Optional<SearchCacheEntry> findByQueryHash(String queryHash);
}
