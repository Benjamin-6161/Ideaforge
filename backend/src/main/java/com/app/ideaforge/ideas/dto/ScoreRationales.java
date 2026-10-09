package com.app.ideaforge.ideas.dto;

import jakarta.validation.constraints.Size;

public record ScoreRationales(
		@Size(max = 600) String demand,
		@Size(max = 600) String profitability,
		@Size(max = 600) String competition,
		@Size(max = 600) String novelty) {
}
