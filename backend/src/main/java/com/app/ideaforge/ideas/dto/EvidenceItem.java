package com.app.ideaforge.ideas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A single piece of research evidence, indexed for citation in scoring. */
public record EvidenceItem(
		@NotBlank @Size(max = 40) String kind,
		@NotBlank @Size(max = 300) String title,
		@NotBlank @Size(max = 2000) String url,
		@Size(max = 500) String snippet) {
}
