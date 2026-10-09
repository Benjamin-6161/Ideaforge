package com.app.ideaforge.finalization;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.app.ideaforge.finalization.dto.FinalizationResponse;
import com.app.ideaforge.security.AuthUser;

@RestController
@RequestMapping("/api/ideas/{ideaId}")
public class FinalizationController {

	private final FinalizationService finalizationService;

	public FinalizationController(FinalizationService finalizationService) {
		this.finalizationService = finalizationService;
	}

	@PostMapping("/finalize")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public FinalizationResponse finalize(
			@AuthenticationPrincipal AuthUser user,
			@PathVariable UUID ideaId,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
		return finalizationService.create(user.id(), ideaId, idempotencyKey);
	}

	@GetMapping("/finalization")
	public FinalizationResponse finalization(
			@AuthenticationPrincipal AuthUser user,
			@PathVariable UUID ideaId) {
		return finalizationService.get(user.id(), ideaId);
	}
}
