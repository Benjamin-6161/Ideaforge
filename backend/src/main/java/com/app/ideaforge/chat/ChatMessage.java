package com.app.ideaforge.chat;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One chat turn message. Immutable except for status/refund bookkeeping. */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

	public enum Role {
		USER,
		ASSISTANT
	}

	public enum Status {
		COMPLETED,
		FAILED
	}

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "idea_id", nullable = false)
	private UUID ideaId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	@Column(nullable = false)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Status status = Status.COMPLETED;

	@Column(name = "resulting_version_id")
	private UUID resultingVersionId;

	@Column(name = "spend_ledger_id")
	private UUID spendLedgerId;

	@Column(name = "refund_ledger_id")
	private UUID refundLedgerId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected ChatMessage() {
	}

	public ChatMessage(UUID ideaId, UUID userId, Role role, String content, UUID spendLedgerId) {
		this.ideaId = ideaId;
		this.userId = userId;
		this.role = role;
		this.content = content;
		this.spendLedgerId = spendLedgerId;
	}

	public static ChatMessage assistant(UUID ideaId, UUID userId, String content, UUID resultingVersionId) {
		ChatMessage message = new ChatMessage(ideaId, userId, Role.ASSISTANT, content, null);
		message.resultingVersionId = resultingVersionId;
		return message;
	}

	public void markFailed() {
		this.status = Status.FAILED;
	}

	public void markRefunded(UUID refundLedgerId) {
		this.refundLedgerId = refundLedgerId;
	}

	public UUID getId() {
		return id;
	}

	public UUID getIdeaId() {
		return ideaId;
	}

	public UUID getUserId() {
		return userId;
	}

	public Role getRole() {
		return role;
	}

	public String getContent() {
		return content;
	}

	public Status getStatus() {
		return status;
	}

	public UUID getResultingVersionId() {
		return resultingVersionId;
	}

	public UUID getSpendLedgerId() {
		return spendLedgerId;
	}

	public UUID getRefundLedgerId() {
		return refundLedgerId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
