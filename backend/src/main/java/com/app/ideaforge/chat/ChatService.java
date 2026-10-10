package com.app.ideaforge.chat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.ideaforge.chat.dto.ChatMessageResponse;
import com.app.ideaforge.chat.dto.ChatTurnResponse;
import com.app.ideaforge.common.error.ApiException;
import com.app.ideaforge.common.error.ErrorCodes;
import com.app.ideaforge.common.web.PageResponse;
import com.app.ideaforge.ideas.IdeaRepository;
import com.app.ideaforge.ideas.IdeaVersion;
import com.app.ideaforge.ideas.IdeaVersionRepository;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Chat orchestration: short transactions around an LLM call that happens
 * outside any transaction. Failed turns refund exactly once.
 */
@Service
public class ChatService {

	private static final Logger log = LoggerFactory.getLogger(ChatService.class);
	private static final int MAX_PAGE_SIZE = 50;

	private final ChatTransactions transactions;
	private final ChatAgent agent;
	private final ChatMessageRepository messages;
	private final IdeaRepository ideas;
	private final IdeaVersionRepository versions;

	public ChatService(
			ChatTransactions transactions,
			ChatAgent agent,
			ChatMessageRepository messages,
			IdeaRepository ideas,
			IdeaVersionRepository versions) {
		this.transactions = transactions;
		this.agent = agent;
		this.messages = messages;
		this.ideas = ideas;
		this.versions = versions;
	}

	public ChatTurnResponse send(
			UUID userId,
			UUID ideaId,
			String idempotencyKey,
			String content,
			HttpServletRequest request) {
		ChatTransactions.Prepared prepared = transactions.prepare(userId, ideaId, idempotencyKey, content, request);
		if (prepared.replay()) {
			return replayResponse(ideaId, prepared.userMessage());
		}
		try {
			ChatAgent.ChatOutcome outcome = agent.respond(prepared.idea(), prepared.userMessage());
			return transactions.complete(
					userId, ideaId, prepared.userMessage().getId(), outcome.replyText(), outcome.revision());
		} catch (RuntimeException ex) {
			String errorCode = ChatAgent.errorCodeFor(ex);
			log.warn("Chat turn failed for idea {} with {}: {}", ideaId, errorCode,
					com.app.ideaforge.llm.LlmErrorClassifier.summary(ex));
			transactions.fail(userId, ideaId, prepared.userMessage().getId(), errorCode);
			throw new ApiException(
					HttpStatus.BAD_GATEWAY,
					ErrorCodes.UPSTREAM_UNAVAILABLE,
					"The agent could not produce a reply. Your credits for this message were refunded.");
		}
	}

	@Transactional(readOnly = true)
	public PageResponse<ChatMessageResponse> list(UUID userId, UUID ideaId, int page, int size) {
		ideas.findByIdAndUserIdAndDeletedAtIsNull(ideaId, userId)
				.orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Idea not found."));
		Page<ChatMessage> result = messages.findByIdeaIdOrderByCreatedAtDesc(
				ideaId, PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, MAX_PAGE_SIZE))));
		Map<UUID, Integer> versionNumbers = versionNumbers(result.getContent());
		return PageResponse.from(result.map(message ->
				ChatMessageResponse.from(message, versionNumbers.get(message.getResultingVersionId()))));
	}

	private ChatTurnResponse replayResponse(UUID ideaId, ChatMessage userMessage) {
		ChatMessage assistant = messages
				.findFirstByIdeaIdAndRoleAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
						ideaId, ChatMessage.Role.ASSISTANT, userMessage.getCreatedAt())
				.filter(candidate -> candidate.getCreatedAt().isAfter(userMessage.getCreatedAt())
						|| candidate.getCreatedAt().equals(userMessage.getCreatedAt()))
				.orElse(null);
		Integer versionNumber = null;
		ChatTurnResponse.VersionRef versionRef = null;
		if (assistant != null && assistant.getResultingVersionId() != null) {
			IdeaVersion version = versions.findById(assistant.getResultingVersionId()).orElse(null);
			if (version != null) {
				versionNumber = version.getVersionNumber();
				versionRef = new ChatTurnResponse.VersionRef(
						version.getId(), version.getVersionNumber(), version.getChangeSummary());
			}
		}
		return new ChatTurnResponse(
				ChatMessageResponse.from(userMessage, null),
				assistant == null ? null : ChatMessageResponse.from(assistant, versionNumber),
				versionRef);
	}

	@Transactional(readOnly = true)
	public List<ChatMessageResponse> recent(UUID userId, UUID ideaId, int limit) {
		ideas.findByIdAndUserIdAndDeletedAtIsNull(ideaId, userId)
				.orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Idea not found."));
		Page<ChatMessage> newestFirst = messages.findByIdeaIdOrderByCreatedAtDesc(
				ideaId, PageRequest.of(0, Math.max(1, Math.min(limit, MAX_PAGE_SIZE))));
		Map<UUID, Integer> versionNumbers = versionNumbers(newestFirst.getContent());
		List<ChatMessageResponse> ascending = new java.util.ArrayList<>(newestFirst.getContent().stream()
				.map(message -> ChatMessageResponse.from(message, versionNumbers.get(message.getResultingVersionId())))
				.toList());
		java.util.Collections.reverse(ascending);
		return List.copyOf(ascending);
	}

	private Map<UUID, Integer> versionNumbers(List<ChatMessage> page) {
		Map<UUID, Integer> numbers = new HashMap<>();
		List<UUID> versionIds = page.stream()
				.map(ChatMessage::getResultingVersionId)
				.filter(java.util.Objects::nonNull)
				.toList();
		for (IdeaVersion version : versions.findAllById(versionIds)) {
			numbers.put(version.getId(), version.getVersionNumber());
		}
		return numbers;
	}
}
