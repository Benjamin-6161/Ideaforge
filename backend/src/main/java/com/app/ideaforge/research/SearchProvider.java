package com.app.ideaforge.research;

import java.util.List;

/** Free web search. Implementations must fail soft (empty list on errors). */
public interface SearchProvider {

	List<SearchResult> search(String query, int maxResults);
}
