package com.app.ideaforge.admin;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.app.ideaforge.IntegrationTestBase;
import com.app.ideaforge.llm.AiUsageLog;
import com.app.ideaforge.llm.AiUsageLogRepository;
import com.app.ideaforge.llm.LlmPurpose;
import com.app.ideaforge.user.EmailNormalizer;
import com.app.ideaforge.user.User;
import com.app.ideaforge.user.UserRepository;
import com.app.ideaforge.user.UserRole;

import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminUsageIntegrationTest extends IntegrationTestBase {

	@Autowired
	UserRepository users;

	@Autowired
	AiUsageLogRepository usageLogs;

	@Autowired
	EmailNormalizer emailNormalizer;

	@Test
	void regularUsersAreForbidden() throws Exception {
		Cookie cookie = login("admin-check-" + UUID.randomUUID() + "@example.com");
		mockMvc.perform(get("/api/admin/usage").cookie(cookie))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminSeesAggregates() throws Exception {
		String email = "admin-" + UUID.randomUUID() + "@example.com";
		Cookie cookie = login(email);
		User user = users.findByEmailNormalized(emailNormalizer.normalize(email)).orElseThrow();
		user.setRole(UserRole.ADMIN);
		users.save(user);

		usageLogs.save(new AiUsageLog(
				user.getId(), LlmPurpose.CHAT, "openai-compatible", "fake-chat",
				1200, 900, 800, 4_200, true, null, null));

		mockMvc.perform(get("/api/admin/usage").param("days", "30").cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.windowDays").value(30))
				.andExpect(jsonPath("$.byPurposeModel[?(@.purpose=='CHAT')].model").value("fake-chat"))
				.andExpect(jsonPath("$.signups.total").isNumber())
				.andExpect(jsonPath("$.credits.grantedUnits").isNumber())
				.andExpect(jsonPath("$.dailyLlmBudgetMicroUsd").value(10_000_000));
	}
}
