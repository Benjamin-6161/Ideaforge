package com.app.ideaforge.credits.dto;

import java.time.Instant;
import java.util.UUID;

import com.app.ideaforge.credits.CreditLedgerEntry;

public record LedgerEntryResponse(
		UUID id,
		String entryType,
		long amountUnits,
		long balanceAfterUnits,
		String action,
		String refType,
		UUID refId,
		Instant createdAt) {

	public static LedgerEntryResponse from(CreditLedgerEntry entry) {
		return new LedgerEntryResponse(
				entry.getId(),
				entry.getEntryType().name(),
				entry.getAmountUnits(),
				entry.getBalanceAfterUnits(),
				entry.getAction(),
				entry.getRefType(),
				entry.getRefId(),
				entry.getCreatedAt());
	}
}
