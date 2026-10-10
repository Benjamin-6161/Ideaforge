package com.app.ideaforge.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.app.ideaforge.chat.dto.IdeaRevisionResponse;
import com.app.ideaforge.ideas.Idea;
import com.app.ideaforge.ideas.IdeaValidation;
import com.app.ideaforge.ideas.IdeaValidationRepository;
import com.app.ideaforge.ideas.IdeaVersion;
import com.app.ideaforge.ideas.IdeaVersionRepository;
import com.app.ideaforge.ideas.dto.IdeaContent;
import com.app.ideaforge.llm.LlmException;
import com.app.ideaforge.llm.LlmGateway;
import com.app.ideaforge.llm.LlmPurpose;
import com.app.ideaforge.llm.LlmToolChatResult;

import jakarta.validation.Validator;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Runs the chat turn against the LLM: tools, delimiter-based revision
 * directive, and (when requested) one structured revision call. No database
 * work happens here so no transaction is held across LLM calls.
 */
@Component
public class ChatAgent {

	private static final int HISTORY_MESSAGES = 10;
	private static final int MAX_TOOL_CALLS = 3;
	private static final String UPDATE_OPEN = "<idea_update>";
	private static final String UPDATE_CLOSE = "</idea_update>";

	private final IdeaVersionRepository versions;
	private final IdeaValidationRepository validations;
	private final ChatMessageRepository messages;
	private final ChatPromptComposer promptComposer;
	private final ChatToolbox toolbox;
	private final LlmGateway llmGateway;
	private final Validator validator;
	private final ObjectMapper objectMapper;

	public ChatAgent(
			IdeaVersionRepository versions,
			IdeaValidationRepository validations,
			ChatMessageRepository messages,
			ChatPromptComposer promptComposer,
			ChatToolbox toolbox,
			LlmGateway llmGateway,
			Validator validator,
			ObjectMapper objectMapper) {
		this.versions = versions;
		this.validations = validations;
		this.messages = messages;
		this.promptComposer = promptComposer;
		this.toolbox = toolbox;
		this.llmGateway = llmGateway;
		this.validator = validator;
		this.objectMapper = objectMapper;
	}

	public record ChatOutcome(String replyText, IdeaRevisionResponse revision) {
	}

	public ChatOutcome respond(Idea idea, ChatMessage userMessage) {
		IdeaContent content = currentContent(idea);
		IdeaValidation validation = validations.findFirstByIdeaIdOrderByCreatedAtDesc(idea.getId()).orElse(null);

		List<ChatMessage> history = messages
				.findByIdeaIdOrderByCreatedAtDesc(idea.getId(), PageRequest.of(0, HISTORY_MESSAGES + 1))
				.stream()
				.filter(message -> !message.getId().equals(userMessage.getId()))
				.limit(HISTORY_MESSAGES)
				.collect(java.util.stream.Collectors.collectingAndThen(
						java.util.stream.Collectors.toList(),
						list -> {
							java.util.Collections.reverse(list);
							return list;
						}));

		LlmToolChatResult result = llmGateway.chatWithTools(
				LlmPurpose.CHAT,
				promptComposer.chatPrompt(content, validation, history, userMessage.getContent()),
				toolbox.tools(),
				MAX_TOOL_CALLS);

		ParsedChat parsed = parse(result.text());
		IdeaRevisionResponse revision = null;
		if (parsed.directive() != null) {
			revision = llmGateway.generateJson(
					LlmPurpose.CHAT,
					promptComposer.revisionPrompt(content, parsed.directive().instructions(), userMessage.getContent()),
					IdeaRevisionResponse.class,
					validator);
		}
		String reply = parsed.visibleText().isBlank()
				? "Done. I updated the idea based on your request."
				: parsed.visibleText();
		return new ChatOutcome(reply, revision);
	}

	private IdeaContent currentContent(Idea idea) {
		if (idea.getCurrentVersionId() == null) {
			throw new LlmException("Idea has no current version");
		}
		return versions.findById(idea.getCurrentVersionId())
				.map(IdeaVersion::getContent)
				.orElseThrow(() -> new LlmException("Idea version missing"));
	}

	record UpdateDirective(String changeSummary, String instructions) {
	}

	record ParsedChat(String visibleText, UpdateDirective directive) {
	}

	ParsedChat parse(String rawText) {
		String text = rawText == null ? "" : rawText;
		int open = text.lastIndexOf(UPDATE_OPEN);
		if (open < 0) {
			return new ParsedChat(text.trim(), null);
		}
		int close = text.indexOf(UPDATE_CLOSE, open);
		if (close < 0) {
			return new ParsedChat(text.substring(0, open).trim(), null);
		}
		String visible = (text.substring(0, open) + text.substring(close + UPDATE_CLOSE.length())).trim();
		String json = text.substring(open + UPDATE_OPEN.length(), close).trim();
		try {
			java.util.Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<java.util.Map<String, Object>>() {
			});
			String changeSummary = parsed.get("changeSummary") == null ? null : String.valueOf(parsed.get("changeSummary"));
			String instructions = parsed.get("instructions") == null ? null : String.valueOf(parsed.get("instructions"));
			if (changeSummary == null || changeSummary.isBlank() || instructions == null || instructions.isBlank()) {
				return new ParsedChat(visible, null);
			}
			return new ParsedChat(visible, new UpdateDirective(changeSummary, instructions));
		} catch (Exception ex) {
			return new ParsedChat(visible, null);
		}
	}

	/** Exposed for tests. */
	static String errorCodeFor(RuntimeException ex) {
		return ex instanceof LlmException llm ? llm.getCode().runCode() : "INTERNAL_ERROR";
	}
}
