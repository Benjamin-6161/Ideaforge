package com.app.ideaforge.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OtpVerifyRequest(
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Pattern(regexp = "\\d{6}", message = "must be 6 digits") String code) {
}
