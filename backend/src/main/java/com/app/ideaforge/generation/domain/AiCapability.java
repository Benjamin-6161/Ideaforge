package com.app.ideaforge.generation.domain;

public enum AiCapability implements LabeledOption {
	TEXT_LLM("Text / LLM"),
	DOCUMENT_OCR("Document understanding / OCR"),
	COMPUTER_VISION("Computer vision"),
	VOICE_SPEECH("Voice / speech"),
	FORECASTING("Forecasting / prediction"),
	RECOMMENDATIONS("Recommendations"),
	WORKFLOW_AGENTS("Workflow agents / automation"),
	DATA_EXTRACTION("Data extraction");

	private final String label;

	AiCapability(String label) {
		this.label = label;
	}

	@Override
	public String label() {
		return label;
	}
}
