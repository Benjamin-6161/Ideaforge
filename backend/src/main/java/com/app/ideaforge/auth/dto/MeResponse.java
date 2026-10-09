package com.app.ideaforge.auth.dto;

import java.util.UUID;

import com.app.ideaforge.user.User;

public record MeResponse(
		UUID id,
		String email,
		String displayName,
		String avatarUrl,
		String role) {

	public static MeResponse from(User user) {
		return new MeResponse(
				user.getId(),
				user.getEmail(),
				user.getDisplayName(),
				user.getAvatarUrl(),
				user.getRole().name());
	}
}
