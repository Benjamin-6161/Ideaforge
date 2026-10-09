package com.app.ideaforge.chat.dto;

import java.time.Instant;
import java.util.UUID;

import com.app.ideaforge.chat.ChatMessage;

public record ChatMessageResponse(
		UUID id,
		String role,
		String content,
		String status,
		UUID resultingVersionId,
		Integer resultingVersionNumber,
		Instant createdAt) {

	public static ChatMessageResponse from(ChatMessage message, Integer resultingVersionNumber) {
		return new ChatMessageResponse(
				message.getId(),
				message.getRole().name(),
				message.getContent(),
				message.getStatus().name(),
				message.getResultingVersionId(),
				resultingVersionNumber,
				message.getCreatedAt());
	}
}
