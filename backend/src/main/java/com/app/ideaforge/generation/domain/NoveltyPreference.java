package com.app.ideaforge.generation.domain;

public enum NoveltyPreference implements LabeledOption {
	FRESH_ANGLE("Proven pain with a fresh angle"),
	BALANCED("Balanced"),
	BOLD("Bold / novel");

	private final String label;

	NoveltyPreference(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}
}
