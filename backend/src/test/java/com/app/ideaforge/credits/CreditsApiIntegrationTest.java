package com.app.ideaforge.credits;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.app.ideaforge.IntegrationTestBase;

import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CreditsApiIntegrationTest extends IntegrationTestBase {

	@Test
	void creditsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/credits")).andExpect(status().isUnauthorized());
	}

	@Test
	void creditsExposeBalanceAndCostsFromDatabase() throws Exception {
		Cookie cookie = login("credits-api-" + UUID.randomUUID() + "@example.com");

		mockMvc.perform(get("/api/credits").cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.balanceUnits").value(50))
				.andExpect(jsonPath("$.costs[?(@.action=='GENERATE_IDEAS')].costUnits").value(20))
				.andExpect(jsonPath("$.costs[?(@.action=='WORK_MESSAGE')].costUnits").value(2))
				.andExpect(jsonPath("$.costs[?(@.action=='FINALIZE')].costUnits").value(10))
				.andExpect(jsonPath("$.costs[?(@.action=='REVALIDATE')].costUnits").value(5));
	}

	@Test
	void ledgerIsScopedToTheAuthenticatedUser() throws Exception {
		String firstEmail = "ledger-a-" + UUID.randomUUID() + "@example.com";
		String secondEmail = "ledger-b-" + UUID.randomUUID() + "@example.com";
		Cookie first = login(firstEmail);
		Cookie second = login(secondEmail);

		mockMvc.perform(get("/api/credits/ledger").cookie(first))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].entryType").value("GRANT_SIGNUP"))
				.andExpect(jsonPath("$.items[0].amountUnits").value(50));

		mockMvc.perform(get("/api/credits/ledger").cookie(second))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1));
	}

	@Test
	void packagesAreListedWithPlaceholderPrices() throws Exception {
		Cookie cookie = login("credits-packages-" + UUID.randomUUID() + "@example.com");

		mockMvc.perform(get("/api/credits/packages").cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(4))
				.andExpect(jsonPath("$[0].code").value("starter"))
				.andExpect(jsonPath("$[0].creditsUnits").value(200));
	}
}
