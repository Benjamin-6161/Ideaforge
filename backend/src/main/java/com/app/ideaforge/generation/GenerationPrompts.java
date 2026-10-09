package com.app.ideaforge.generation;

import java.util.List;

import org.springframework.stereotype.Component;

import com.app.ideaforge.fields.Field;
import com.app.ideaforge.generation.dto.GenerationParameters;
import com.app.ideaforge.ideas.dto.EvidenceItem;
import com.app.ideaforge.ideas.dto.IdeaContent;
import com.app.ideaforge.llm.LlmPrompt;
import com.app.ideaforge.llm.PromptLibrary;

import tools.jackson.databind.ObjectMapper;

/**
 * Composes user prompts with clearly delimited untrusted blocks; all rules
 * live in the versioned system prompts.
 */
@Component
public class GenerationPrompts {

	private final PromptLibrary promptLibrary;
	private final ObjectMapper objectMapper;

	public GenerationPrompts(PromptLibrary promptLibrary, ObjectMapper objectMapper) {
		this.promptLibrary = promptLibrary;
		this.objectMapper = objectMapper;
	}

	public String ideationVersion() {
		return promptLibrary.load("ideation-system").versionLabel();
	}

	public String scoringVersion() {
		return promptLibrary.load("scoring-system").versionLabel();
	}

	public LlmPrompt ideation(
			Field field,
			String niche,
			GenerationParameters parameters,
			List<String> avoidTitles,
			int count) {
		String system = promptLibrary.load("ideation-system").content();
		StringBuilder user = new StringBuilder();
		user.append("<user_input>\n");
		user.append("field: ").append(oneLine(field.getName())).append("\n");
		if (field.getDescription() != null && !field.getDescription().isBlank()) {
			user.append("field_scope: ").append(oneLine(field.getDescription())).append("\n");
		}
		if (niche != null && !niche.isBlank()) {
			user.append("niche: ").append(oneLine(niche)).append("\n");
		}
		appendIfPresent(user, "target_customer", label(parameters.targetCustomerOrDefault()));
		appendIfPresent(user, "customer_size", label(parameters.customerSizeOrDefault()));
		appendIfPresent(user, "builder_profile", label(parameters.builderProfileOrDefault()));
		appendIfPresent(user, "time_to_mvp", label(parameters.timeToMvpOrDefault()));
		appendIfPresent(user, "starting_budget", label(parameters.startingBudgetOrDefault()));
		appendIfPresent(user, "ai_depth", label(parameters.aiDepthOrDefault()));
		if (!parameters.aiCapabilities().isEmpty()) {
			user.append("ai_capabilities: ")
					.append(String.join(", ", parameters.aiCapabilities().stream().map(this::label).toList()))
					.append("\n");
		}
		appendIfPresent(user, "monetization", label(parameters.monetizationOrDefault()));
		appendIfPresent(user, "region", label(parameters.regionOrDefault()));
		appendIfPresent(user, "novelty_preference", label(parameters.noveltyPreferenceOrDefault()));
		if (parameters.problemFocus() != null && !parameters.problemFocus().isBlank()) {
			user.append("problem_focus: ").append(oneLine(parameters.problemFocus())).append("\n");
		}
		if (parameters.exclude() != null && !parameters.exclude().isBlank()) {
			user.append("exclude: ").append(oneLine(parameters.exclude())).append("\n");
		}
		user.append("ideas_requested: ").append(count).append("\n");
		user.append("</user_input>\n\n");

		if (!avoidTitles.isEmpty()) {
			user.append("<avoid_titles>\n")
					.append(objectMapper.writeValueAsString(avoidTitles))
					.append("\n</avoid_titles>\n\n");
		}
		user.append("Produce ").append(count).append(" idea(s) now as a single JSON object.");
		return new LlmPrompt(system, user.toString());
	}

	public LlmPrompt scoring(List<IdeaContent> ideas, List<ResearchBundle> bundles) {
		String system = promptLibrary.load("scoring-system").content();
		StringBuilder user = new StringBuilder();
		user.append("<user_input>\n");
		for (int i = 0; i < ideas.size(); i++) {
			IdeaContent idea = ideas.get(i);
			ResearchBundle bundle = bundles.get(i);
			user.append("idea_index: ").append(i).append("\n");
			user.append("title: ").append(oneLine(idea.title())).append("\n");
			user.append("tagline: ").append(oneLine(idea.tagline())).append("\n");
			user.append("summary: ").append(oneLine(idea.summary())).append("\n");
			user.append("target_customer: ").append(oneLine(idea.targetCustomer().persona())).append("\n");
			user.append("problem: ").append(oneLine(idea.problem().statement())).append("\n");
			bundleFrom(user, bundle);
			user.append("\n");
		}
		user.append("</user_input>\n\n");
		user.append("Score every idea above. Respond with the JSON object only.");
		return new LlmPrompt(system, user.toString());
	}

	private void bundleFrom(StringBuilder user, ResearchBundle bundle) {
		if (bundle == null || bundle.evidence().isEmpty()) {
			user.append("evidence: none available\n");
			return;
		}
		user.append("<untrusted_web_content>\n");
		List<EvidenceItem> evidence = bundle.evidence();
		for (int index = 0; index < evidence.size(); index++) {
			EvidenceItem item = evidence.get(index);
			user.append("[").append(index).append("] ")
					.append(item.kind()).append(" | ")
					.append(oneLine(item.title())).append(" | ")
					.append(oneLine(item.url()));
			if (item.snippet() != null && !item.snippet().isBlank()) {
				user.append(" | ").append(oneLine(item.snippet()));
			}
			user.append("\n");
		}
		user.append("</untrusted_web_content>\n");
	}

	private void appendIfPresent(StringBuilder user, String key, String value) {
		if (value != null && !value.isBlank()) {
			user.append(key).append(": ").append(value).append("\n");
		}
	}

	private String label(com.app.ideaforge.generation.domain.LabeledOption option) {
		return option == null ? null : oneLine(option.label());
	}

	/** Delimiters and newlines are stripped from single-line values. */
	private String oneLine(String value) {
		if (value == null) {
			return "";
		}
		return value.replaceAll("[\\r\\n]+", " ")
				.replace("<user_input>", "")
				.replace("</user_input>", "")
				.replace("<untrusted_web_content>", "")
				.replace("</untrusted_web_content>", "")
				.trim();
	}
}
