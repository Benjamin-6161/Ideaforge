package com.app.ideaforge.chat;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.ideaforge.chat.dto.ChatMessageResponse;
import com.app.ideaforge.chat.dto.ChatTurnResponse;
import com.app.ideaforge.chat.dto.IdeaRevisionResponse;
import com.app.ideaforge.common.config.AppProperties;
import com.app.ideaforge.common.error.ApiException;
import com.app.ideaforge.common.error.ErrorCodes;
import com.app.ideaforge.credits.CreditLedgerEntry;
import com.app.ideaforge.credits.CreditService;
import com.app.ideaforge.ideas.Idea;
import com.app.ideaforge.ideas.IdeaRepository;
import com.app.ideaforge.ideas.IdeaStatus;
import com.app.ideaforge.ideas.IdeaVersion;
import com.app.ideaforge.ideas.IdeaVersionRepository;
import com.app.ideaforge.ideas.VersionSource;
import com.app.ideaforge.security.ClientIpHasher;
import com.app.ideaforge.security.InputGuard;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Short transactional steps around the chat LLM call: prepare (validate,
 * charge, store the user message), complete (store the reply and any new
 * version), fail (refund and mark the turn failed).
 */
@Service
public class ChatTransactions {

	private final IdeaRepository ideas;
	private final IdeaVersionRepository versions;
	private final ChatMessageRepository messages;
	private final CreditService creditService;
	private final InputGuard inputGuard;
	private final ClientIpHasher ipHasher;
	private final AppProperties properties;
	private final Clock clock;

	public ChatTransactions(
			IdeaRepository ideas,
			IdeaVersionRepository versions,
			ChatMessageRepository messages,
			CreditService creditService,
			InputGuard inputGuard,
			ClientIpHasher ipHasher,
			AppProperties properties,
			Clock clock) {
		this.ideas = ideas;
		this.versions = versions;
		this.messages = messages;
		this.creditService = creditService;
		this.inputGuard = inputGuard;
		this.ipHasher = ipHasher;
		this.properties = properties;
		this.clock = clock;
	}

	public record Prepared(Idea idea, ChatMessage userMessage, boolean replay) {
	}

	@Transactional
	public Prepared prepare(
			UUID userId,
			UUID ideaId,
			String idempotencyKey,
			String rawContent,
			HttpServletRequest request) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw ApiException.badRequest(ErrorCodes.IDEMPOTENCY_KEY_REQUIRED,
					"An Idempotency-Key header is required for this endpoint.");
		}
		if (idempotencyKey.length() > 100) {
			throw ApiException.badRequest(ErrorCodes.VALIDATION_ERROR, "Idempotency-Key is too long.");
		}
		Idea idea = ideas.findByIdAndUserIdAndDeletedAtIsNull(ideaId, userId)
				.orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Idea not found."));
		if (idea.getStatus() == IdeaStatus.FINALIZED) {
			throw ApiException.conflict(ErrorCodes.IDEA_LOCKED,
					"This idea is finalized and can no longer be edited.");
		}

		Optional<CreditLedgerEntry> previousSpend = creditService.findByIdempotencyKey(userId, idempotencyKey);
		if (previousSpend.isPresent()) {
			Optional<ChatMessage> previousMessage = messages.findBySpendLedgerId(previousSpend.get().getId());
			if (previousMessage.isPresent()) {
				return new Prepared(idea, previousMessage.get(), true);
			}
		}

		long perIdea = messages.countByIdeaIdAndRole(idea.getId(), ChatMessage.Role.USER);
		if (perIdea >= properties.limits().maxChatMessagesPerIdea()) {
			throw ApiException.conflict(ErrorCodes.MESSAGE_LIMIT_REACHED,
					"This idea has reached its chat message limit. Finalize it to get the build pack.");
		}
		long today = messages.countByUserIdAndRoleAndCreatedAtAfter(
				userId,
				ChatMessage.Role.USER,
				clock.instant().truncatedTo(ChronoUnit.DAYS));
		if (today >= properties.limits().chatMessagesPerDay()) {
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, ErrorCodes.DAILY_LIMIT_REACHED,
					"Daily chat limit reached. Try again tomorrow.");
		}

		String ipHash = ipHasher.hash(request);
		String clean = inputGuard.guardText(rawContent, 2000, userId, ipHash, "chat_message");

		CreditLedgerEntry spend = creditService.spend(
				userId, "WORK_MESSAGE", "chat", ideaId, idempotencyKey);
		Optional<ChatMessage> raced = messages.findBySpendLedgerId(spend.getId());
		if (raced.isPresent()) {
			return new Prepared(idea, raced.get(), true);
		}
		ChatMessage userMessage = messages.save(
				new ChatMessage(ideaId, userId, ChatMessage.Role.USER, clean, spend.getId()));
		idea.incrementMessageCount();
		return new Prepared(idea, userMessage, false);
	}

	@Transactional
	public ChatTurnResponse complete(
			UUID userId,
			UUID ideaId,
			UUID userMessageId,
			String replyText,
			IdeaRevisionResponse revision) {
		ChatMessage userMessage = messages.findById(userMessageId)
				.orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Message not found."));
		Idea idea = ideas.findByIdAndUserIdAndDeletedAtIsNull(ideaId, userId)
				.orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Idea not found."));

		ChatTurnResponse.VersionRef versionRef = null;
		UUID resultingVersionId = null;
		if (revision != null) {
			int nextNumber = versions.maxVersionNumber(ideaId) + 1;
			IdeaVersion version = versions.save(new IdeaVersion(
					ideaId,
					nextNumber,
					VersionSource.CHAT,
					revision.idea(),
					revision.changeSummary(),
					userMessageId));
			idea.applyContent(revision.idea().title(), revision.idea().tagline(), version.getId());
			if (idea.getStatus() == IdeaStatus.GENERATED) {
				idea.setStatus(IdeaStatus.IN_PROGRESS);
			}
			resultingVersionId = version.getId();
			versionRef = new ChatTurnResponse.VersionRef(
					version.getId(), version.getVersionNumber(), revision.changeSummary());
		}

		ChatMessage assistant = messages.save(ChatMessage.assistant(ideaId, userId, replyText, resultingVersionId));
		return new ChatTurnResponse(
				ChatMessageResponse.from(userMessage, null),
				ChatMessageResponse.from(assistant, versionRef == null ? null : versionRef.versionNumber()),
				versionRef);
	}

	@Transactional
	public void fail(UUID userId, UUID ideaId, UUID userMessageId, String errorCode) {
		ChatMessage userMessage = messages.findById(userMessageId).orElse(null);
		if (userMessage == null) {
			return;
		}
		if (userMessage.getRefundLedgerId() == null && userMessage.getSpendLedgerId() != null) {
			creditService.refund(userMessage.getSpendLedgerId(), "Chat failed (" + errorCode + ")")
					.ifPresent(refund -> userMessage.markRefunded(refund.getId()));
		}
		ChatMessage assistant = ChatMessage.assistant(
				ideaId,
				userId,
				"The agent could not produce a reply. Your credits for this message were refunded.",
				null);
		assistant.markFailed();
		messages.save(assistant);
	}
}
