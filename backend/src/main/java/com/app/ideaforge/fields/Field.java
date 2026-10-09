package com.app.ideaforge.fields;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fields")
public class Field {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "parent_id")
	private UUID parentId;

	@Column(nullable = false, unique = true)
	private String slug;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String description;

	@Column(nullable = false)
	private String icon;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected Field() {
	}

	public UUID getId() {
		return id;
	}

	public UUID getParentId() {
		return parentId;
	}

	public String getSlug() {
		return slug;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getIcon() {
		return icon;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
