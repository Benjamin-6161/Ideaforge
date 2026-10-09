package com.app.ideaforge.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleAuthRequest(
		@NotBlank @Size(max = 4096) String idToken,
		@Size(max = 2048) String turnstileToken) {
}
