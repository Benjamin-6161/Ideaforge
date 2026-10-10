package com.app.ideaforge.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptLibraryTest {

	private final PromptLibrary library = new PromptLibrary();

	@Test
	void loadsHighestVersionWithLabel() {
		PromptLibrary.PromptTemplate template = library.load("ideation-system");
		assertThat(template.version()).isEqualTo(2);
		assertThat(template.versionLabel()).isEqualTo("ideation-system-v2");
		assertThat(template.content()).contains("Idea quality rubric").contains("Hard length limits");
	}

	@Test
	void scoringPromptDefinesScales() {
		String content = library.load("scoring-system").content();
		assertThat(content).contains("demandScore").contains("competitionScore").contains("Hard length limits");
	}

	@Test
	void failsForUnknownPrompt() {
		assertThatThrownBy(() -> library.load("missing-system"))
				.isInstanceOf(LlmException.class)
				.hasMessageContaining("No prompt file");
	}
}
