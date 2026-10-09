package com.app.ideaforge.credits;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.ideaforge.common.web.PageResponse;
import com.app.ideaforge.credits.dto.CreditPackageResponse;
import com.app.ideaforge.credits.dto.CreditsResponse;
import com.app.ideaforge.credits.dto.LedgerEntryResponse;
import com.app.ideaforge.security.AuthUser;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/credits")
public class CreditController {

	private final CreditService creditService;

	public CreditController(CreditService creditService) {
		this.creditService = creditService;
	}

	@GetMapping
	public CreditsResponse credits(@AuthenticationPrincipal AuthUser user) {
		return CreditsResponse.of(creditService.balanceUnits(user.id()), creditService.activeCosts());
	}

	@GetMapping("/ledger")
	public PageResponse<LedgerEntryResponse> ledger(
			@AuthenticationPrincipal AuthUser user,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return PageResponse.from(creditService
				.ledgerPage(user.id(), page, size)
				.map(LedgerEntryResponse::from));
	}

	@GetMapping("/packages")
	public List<CreditPackageResponse> packages() {
		return creditService.activePackages().stream().map(CreditPackageResponse::from).toList();
	}
}
