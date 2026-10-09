package com.app.ideaforge.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.ideaforge.admin.AdminUsageService.AdminUsageResponse;

/** Admin-only observability. Access is enforced by the security filter chain. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

	private final AdminUsageService adminUsageService;

	public AdminController(AdminUsageService adminUsageService) {
		this.adminUsageService = adminUsageService;
	}

	@GetMapping("/usage")
	public AdminUsageResponse usage(@RequestParam(defaultValue = "30") int days) {
		return adminUsageService.usage(days);
	}
}
