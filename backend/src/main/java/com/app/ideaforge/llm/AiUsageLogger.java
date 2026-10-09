package com.app.ideaforge.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Persists usage rows in their own transaction; never fails the LLM call. */
@Service
public class AiUsageLogger {

	private static final Logger log = LoggerFactory.getLogger(AiUsageLogger.class);

	private final AiUsageLogRepository repository;

	public AiUsageLogger(AiUsageLogRepository repository) {
		this.repository = repository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(AiUsageLog usageLog) {
		try {
			repository.save(usageLog);
		} catch (Exception ex) {
			log.error("Failed to persist AI usage log: {}", ex.getMessage());
		}
	}

	/** Convenience factory for the gateway. */
	public void record(
			LlmPurpose purpose,
			String provider,
			String model,
			int tokensIn,
			int tokensOut,
			int latencyMs,
			long estimatedCostMicroUsd,
			boolean success) {
		record(new AiUsageLog(
				null, purpose, provider, model, tokensIn, tokensOut, latencyMs, estimatedCostMicroUsd, success, null, null));
	}
}
