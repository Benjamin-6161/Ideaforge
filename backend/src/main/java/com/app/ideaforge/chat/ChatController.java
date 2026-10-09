package com.app.ideaforge.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.ideaforge.chat.dto.ChatMessageResponse;
import com.app.ideaforge.chat.dto.ChatTurnResponse;
import com.app.ideaforge.chat.dto.SendMessageRequest;
import com.app.ideaforge.common.web.PageResponse;
import com.app.ideaforge.security.AuthUser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ideas/{ideaId}/messages")
public class ChatController {

	private final ChatService chatService;

	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	@PostMapping
	public ChatTurnResponse send(
			@AuthenticationPrincipal AuthUser user,
			@PathVariable UUID ideaId,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@Valid @RequestBody SendMessageRequest body,
			HttpServletRequest request) {
		return chatService.send(user.id(), ideaId, idempotencyKey, body.content(), request);
	}

	@GetMapping
	public PageResponse<ChatMessageResponse> list(
			@AuthenticationPrincipal AuthUser user,
			@PathVariable UUID ideaId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return chatService.list(user.id(), ideaId, page, size);
	}

	/** Convenience for the work view: the latest messages in reading order. */
	@GetMapping("/recent")
	public List<ChatMessageResponse> recent(
			@AuthenticationPrincipal AuthUser user,
			@PathVariable UUID ideaId,
			@RequestParam(defaultValue = "50") int limit) {
		return chatService.recent(user.id(), ideaId, limit);
	}
}
