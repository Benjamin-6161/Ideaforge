package com.app.ideaforge.research;

/** One web search result (title/url/snippet only; pages are fetched separately). */
public record SearchResult(String title, String url, String snippet) {
}
