package com.app.ideaforge.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OtpRequest(
		@NotBlank @Email @Size(max = 254) String email,
		@Size(max = 2048) String turnstileToken) {
}
