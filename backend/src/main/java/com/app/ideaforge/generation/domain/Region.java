package com.app.ideaforge.generation.domain;

public enum Region implements LabeledOption {
	GLOBAL("Global"),
	NORTH_AMERICA("North America"),
	EUROPE("Europe"),
	SOUTH_ASIA("South Asia"),
	MENA("MENA"),
	AFRICA("Africa"),
	LATIN_AMERICA("Latin America"),
	SOUTHEAST_ASIA("Southeast Asia");

	private final String label;

	Region(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}
}
