package com.app.ideaforge.generation.domain;

public enum TimeToMvp implements LabeledOption {
	UNDER_2_WEEKS("Under 2 weeks"),
	ABOUT_1_MONTH("About 1 month"),
	TWO_TO_THREE_MONTHS("2-3 months"),
	NO_LIMIT("No limit");

	private final String label;

	TimeToMvp(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}
}
