package com.app.ideaforge.chat.dto;

import java.util.UUID;

public record ChatTurnResponse(
		ChatMessageResponse userMessage,
		ChatMessageResponse assistantMessage,
		VersionRef newVersion) {

	public record VersionRef(UUID id, int versionNumber, String changeSummary) {
	}
}
