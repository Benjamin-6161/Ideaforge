package com.app.ideaforge.generation.domain;

public enum TargetCustomer implements LabeledOption {
	B2B("B2B"),
	B2C("B2C"),
	B2B2C("B2B2C"),
	FREELANCERS_PROSUMERS("Freelancers / prosumers"),
	ANY("Any");

	private final String label;

	TargetCustomer(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}
}
