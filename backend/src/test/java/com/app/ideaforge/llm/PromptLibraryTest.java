package com.app.ideaforge.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptLibraryTest {

	private final PromptLibrary library = new PromptLibrary();

	@Test
	void loadsHighestVersionWithLabel() {
		PromptLibrary.PromptTemplate template = library.load("ideation-system");
		assertThat(template.version()).isEqualTo(1);
		assertThat(template.versionLabel()).isEqualTo("ideation-system-v1");
		assertThat(template.content()).contains("Idea quality rubric");
	}

	@Test
	void scoringPromptDefinesScales() {
		String content = library.load("scoring-system").content();
		assertThat(content).contains("demandScore").contains("competitionScore");
	}

	@Test
	void failsForUnknownPrompt() {
		assertThatThrownBy(() -> library.load("missing-system"))
				.isInstanceOf(LlmException.class)
				.hasMessageContaining("No prompt file");
	}
}
