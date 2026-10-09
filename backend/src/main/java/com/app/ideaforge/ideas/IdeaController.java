package com.app.ideaforge.ideas;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.ideaforge.common.web.PageResponse;
import com.app.ideaforge.ideas.dto.IdeaDetail;
import com.app.ideaforge.ideas.dto.IdeaListItem;
import com.app.ideaforge.ideas.dto.PatchIdeaRequest;
import com.app.ideaforge.ideas.dto.VersionSummary;
import com.app.ideaforge.security.AuthUser;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/ideas")
public class IdeaController {

	private final IdeaService ideaService;

	public IdeaController(IdeaService ideaService) {
		this.ideaService = ideaService;
	}

	@GetMapping
	public PageResponse<IdeaListItem> list(
			@AuthenticationPrincipal AuthUser user,
			@RequestParam(required = false) UUID fieldId,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Boolean favorite,
			@RequestParam(defaultValue = "false") boolean archived,
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "newest") String sort,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ideaService.list(user.id(), fieldId, status, favorite, archived, q, sort, page, size);
	}

	@GetMapping("/{id}")
	public IdeaDetail detail(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
		return ideaService.detail(user.id(), id);
	}

	@PatchMapping("/{id}")
	public IdeaDetail patch(
			@AuthenticationPrincipal AuthUser user,
			@PathVariable UUID id,
			@Valid @RequestBody PatchIdeaRequest request) {
		return ideaService.patch(user.id(), id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
		ideaService.softDelete(user.id(), id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/versions")
	public List<VersionSummary> versions(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
		return ideaService.versions(user.id(), id);
	}

	@GetMapping("/{id}/versions/{number}")
	public VersionSummary version(
			@AuthenticationPrincipal AuthUser user,
			@PathVariable UUID id,
			@PathVariable int number) {
		return ideaService.version(user.id(), id, number);
	}

	@PostMapping("/{id}/versions/{number}/restore")
	public VersionSummary restore(
			@AuthenticationPrincipal AuthUser user,
			@PathVariable UUID id,
			@PathVariable int number) {
		return ideaService.restore(user.id(), id, number);
	}
}
