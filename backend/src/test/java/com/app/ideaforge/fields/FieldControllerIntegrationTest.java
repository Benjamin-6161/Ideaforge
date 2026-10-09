package com.app.ideaforge.fields;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.app.ideaforge.IntegrationTestBase;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FieldControllerIntegrationTest extends IntegrationTestBase {

	@Test
	void fieldsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/fields"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void fieldsIncludeNiches() throws Exception {
		Cookie cookie = login("fields-" + java.util.UUID.randomUUID() + "@example.com");

		MvcResult result = mockMvc.perform(get("/api/fields").cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(32))
				.andExpect(jsonPath("$[0].name").value("Healthcare & Clinics"))
				.andExpect(jsonPath("$[0].niches.length()").value(5))
				.andReturn();
		assertThat(result.getResponse().getContentAsString()).contains("healthcare-clinics-medical-billing");
	}

	@Test
	void generationOptionsExposeEnumValues() throws Exception {
		Cookie cookie = login("options-" + java.util.UUID.randomUUID() + "@example.com");

		mockMvc.perform(get("/api/generation-options").cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.targetCustomer[0].value").value("B2B"))
				.andExpect(jsonPath("$.aiCapabilities.length()").value(8))
				.andExpect(jsonPath("$.region.length()").value(8));
	}
}
