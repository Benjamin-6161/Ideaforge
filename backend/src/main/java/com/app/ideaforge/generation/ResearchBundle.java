package com.app.ideaforge.generation;

import java.util.List;

import com.app.ideaforge.ideas.dto.EvidenceItem;

/** Light-check research output for one idea. */
public record ResearchBundle(List<EvidenceItem> evidence, List<String> queries) {
}
