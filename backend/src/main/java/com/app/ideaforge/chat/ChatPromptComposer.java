package com.app.ideaforge.chat;

import java.util.List;

import org.springframework.stereotype.Component;

import com.app.ideaforge.ideas.IdeaValidation;
import com.app.ideaforge.ideas.dto.IdeaContent;
import com.app.ideaforge.llm.LlmPrompt;
import com.app.ideaforge.llm.PromptLibrary;

import tools.jackson.databind.ObjectMapper;

/**
 * Builds chat prompts: static system rules plus delimited untrusted blocks
 * (idea content, validation summary, conversation, user request).
 */
@Component
public class ChatPromptComposer {

	private static final int HISTORY_MESSAGE_CHARS = 800;

	private final PromptLibrary promptLibrary;
	private final ObjectMapper objectMapper;

	public ChatPromptComposer(PromptLibrary promptLibrary, ObjectMapper objectMapper) {
		this.promptLibrary = promptLibrary;
		this.objectMapper = objectMapper;
	}

	public String chatVersion() {
		return promptLibrary.load("chat-system").versionLabel();
	}

	public LlmPrompt chatPrompt(
			IdeaContent content,
			IdeaValidation validation,
			List<ChatMessage> history,
			String userMessage) {
		String system = promptLibrary.load("chat-system").content();
		StringBuilder user = new StringBuilder();
		user.append("<user_input>\n");
		user.append("idea_title: ").append(content.title()).append("\n");
		user.append("</user_input>\n\n");

		user.append("<idea_content>\n")
				.append(objectMapper.writeValueAsString(content))
				.append("\n</idea_content>\n\n");

		user.append("<latest_validation>\n");
		if (validation == null) {
			user.append("no validation yet\n");
		} else {
			user.append("demand ").append(validation.getDemandScore()).append("/10: ")
					.append(oneLine(validation.getRationale().demand())).append("\n");
			user.append("profitability ").append(validation.getProfitabilityScore()).append("/10: ")
					.append(oneLine(validation.getRationale().profitability())).append("\n");
			user.append("competition ").append(validation.getCompetitionScore()).append("/10 (10 = saturated): ")
					.append(oneLine(validation.getRationale().competition())).append("\n");
			user.append("novelty ").append(validation.getNoveltyScore()).append("/10: ")
					.append(oneLine(validation.getRationale().novelty())).append("\n");
			user.append("evidence: ").append(validation.getEvidence().size()).append(" sources")
					.append(validation.isEvidenceAvailable() ? "\n" : " (unavailable)\n");
		}
		user.append("</latest_validation>\n\n");

		if (!history.isEmpty()) {
			user.append("<conversation>\n");
			for (ChatMessage message : history) {
				user.append(message.getRole() == ChatMessage.Role.USER ? "user: " : "assistant: ")
						.append(oneLine(truncate(message.getContent(), HISTORY_MESSAGE_CHARS)))
						.append("\n");
			}
			user.append("</conversation>\n\n");
		}

		user.append("<user_request>\n")
				.append(userMessage)
				.append("\n</user_request>");
		return new LlmPrompt(system, user.toString());
	}

	public LlmPrompt revisionPrompt(IdeaContent content, String instructions, String originalRequest) {
		String system = promptLibrary.load("revision-system").content();
		String user = "<user_input>\n"
				+ "requested_change: " + oneLine(instructions) + "\n"
				+ "original_request: " + oneLine(truncate(originalRequest, 500)) + "\n"
				+ "</user_input>\n\n"
				+ "<idea_content>\n" + objectMapper.writeValueAsString(content) + "\n</idea_content>\n\n"
				+ "Apply the change and respond with the JSON object only.";
		return new LlmPrompt(system, user);
	}

	private String truncate(String value, int max) {
		if (value == null) {
			return "";
		}
		return value.length() <= max ? value : value.substring(0, max);
	}

	private String oneLine(String value) {
		return value == null ? "" : value.replaceAll("[\\r\\n]+", " ").trim();
	}
}
