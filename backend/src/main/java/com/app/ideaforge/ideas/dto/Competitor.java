package com.app.ideaforge.ideas.dto;

import jakarta.validation.constraints.Size;

public record Competitor(
		@Size(max = 160) String name,
		@Size(max = 2000) String url,
		@Size(max = 300) String note) {
}
