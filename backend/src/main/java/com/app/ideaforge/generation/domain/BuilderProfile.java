package com.app.ideaforge.generation.domain;

public enum BuilderProfile implements LabeledOption {
	SOLO_FOUNDER("Solo founder"),
	SMALL_TEAM("Small team (2-5)"),
	FUNDED_STARTUP("Funded startup");

	private final String label;

	BuilderProfile(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}
}
