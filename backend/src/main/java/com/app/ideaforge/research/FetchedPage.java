package com.app.ideaforge.research;

/** Extracted text of a fetched page. */
public record FetchedPage(String url, String title, String text, boolean truncated) {
}
