package com.app.ideaforge.generation.domain;

public enum Monetization implements LabeledOption {
	SUBSCRIPTION("Subscription"),
	USAGE_BASED("Usage-based"),
	PER_SEAT("Per-seat"),
	MARKETPLACE_COMMISSION("Marketplace / commission"),
	FREEMIUM("Freemium"),
	ANY("Any");

	private final String label;

	Monetization(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}
}
