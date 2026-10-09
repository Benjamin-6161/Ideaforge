package com.app.ideaforge.generation;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.app.ideaforge.generation.dto.CreateGenerationRequest;
import com.app.ideaforge.generation.dto.GenerationRunResponse;
import com.app.ideaforge.security.AuthUser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/generations")
public class GenerationController {

	private final GenerationService generationService;

	public GenerationController(GenerationService generationService) {
		this.generationService = generationService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	public GenerationRunResponse create(
			@AuthenticationPrincipal AuthUser user,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@Valid @RequestBody CreateGenerationRequest body,
			HttpServletRequest request) {
		return generationService.create(user.id(), idempotencyKey, body, request);
	}

	@GetMapping("/{id}")
	public GenerationRunResponse get(
			@AuthenticationPrincipal AuthUser user,
			@PathVariable UUID id) {
		return generationService.get(user.id(), id);
	}
}
