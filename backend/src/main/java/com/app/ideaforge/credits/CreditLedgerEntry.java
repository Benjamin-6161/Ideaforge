package com.app.ideaforge.credits;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Append-only ledger row. Every wallet change writes exactly one row in the
 * same transaction; rows are never updated or deleted.
 */
@Entity
@Table(name = "credit_ledger")
public class CreditLedgerEntry {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "entry_type", nullable = false)
	private LedgerEntryType entryType;

	/** Signed: positive for grants/refunds, negative for spends. */
	@Column(name = "amount_units", nullable = false)
	private long amountUnits;

	@Column(name = "balance_after_units", nullable = false)
	private long balanceAfterUnits;

	private String action;

	@Column(name = "ref_type")
	private String refType;

	@Column(name = "ref_id")
	private UUID refId;

	@Column(name = "related_entry_id")
	private UUID relatedEntryId;

	@Column(name = "idempotency_key")
	private String idempotencyKey;

	private String note;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected CreditLedgerEntry() {
	}

	public CreditLedgerEntry(
			UUID userId,
			LedgerEntryType entryType,
			long amountUnits,
			long balanceAfterUnits,
			String action,
			String refType,
			UUID refId,
			UUID relatedEntryId,
			String idempotencyKey,
			String note) {
		this.userId = userId;
		this.entryType = entryType;
		this.amountUnits = amountUnits;
		this.balanceAfterUnits = balanceAfterUnits;
		this.action = action;
		this.refType = refType;
		this.refId = refId;
		this.relatedEntryId = relatedEntryId;
		this.idempotencyKey = idempotencyKey;
		this.note = note;
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public LedgerEntryType getEntryType() {
		return entryType;
	}

	public long getAmountUnits() {
		return amountUnits;
	}

	public long getBalanceAfterUnits() {
		return balanceAfterUnits;
	}

	public String getAction() {
		return action;
	}

	public String getRefType() {
		return refType;
	}

	public UUID getRefId() {
		return refId;
	}

	public UUID getRelatedEntryId() {
		return relatedEntryId;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public String getNote() {
		return note;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
