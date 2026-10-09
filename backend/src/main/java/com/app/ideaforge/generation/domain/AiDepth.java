package com.app.ideaforge.generation.domain;

public enum AiDepth implements LabeledOption {
	AI_CORE("AI-core (impossible without AI)"),
	AI_ENHANCED("AI-enhanced"),
	ANY("Any");

	private final String label;

	AiDepth(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}
}
