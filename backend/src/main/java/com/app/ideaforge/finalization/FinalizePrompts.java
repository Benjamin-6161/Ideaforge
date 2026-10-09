package com.app.ideaforge.finalization;

import com.app.ideaforge.finalization.dto.FinalizePlan;
import com.app.ideaforge.ideas.dto.IdeaContent;
import com.app.ideaforge.llm.LlmPrompt;
import com.app.ideaforge.llm.PromptLibrary;

import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

/** Prompts for the finalization calls (plan, build prompt, AGENTS.md). */
@Component
public class FinalizePrompts {

	private final PromptLibrary promptLibrary;
	private final ObjectMapper objectMapper;

	public FinalizePrompts(PromptLibrary promptLibrary, ObjectMapper objectMapper) {
		this.promptLibrary = promptLibrary;
		this.objectMapper = objectMapper;
	}

	public String version() {
		return promptLibrary.load("finalize-system").versionLabel();
	}

	public LlmPrompt plan(IdeaContent content, String validationSummary) {
		String system = promptLibrary.load("finalize-system").content();
		String user = "<idea_content>\n" + objectMapper.writeValueAsString(content) + "\n</idea_content>\n\n"
				+ "<validation>\n" + validationSummary + "\n</validation>\n\n"
				+ "Respond with the JSON object only.";
		return new LlmPrompt(system, user);
	}

	public LlmPrompt buildPrompt(IdeaContent content, FinalizePlan plan) {
		String system = promptLibrary.load("finalize-build-prompt").content();
		String user = "<idea_content>\n" + objectMapper.writeValueAsString(content) + "\n</idea_content>\n\n"
				+ "<plan>\n" + objectMapper.writeValueAsString(plan) + "\n</plan>\n\n"
				+ "Write the build prompt document now.";
		return new LlmPrompt(system, user);
	}

	public LlmPrompt agentsMd(IdeaContent content, FinalizePlan plan) {
		String system = promptLibrary.load("finalize-agents").content();
		String user = "<idea_content>\n" + objectMapper.writeValueAsString(content) + "\n</idea_content>\n\n"
				+ "<plan>\n" + objectMapper.writeValueAsString(plan) + "\n</plan>\n\n"
				+ "Write the AGENTS.md file now.";
		return new LlmPrompt(system, user);
	}
}
