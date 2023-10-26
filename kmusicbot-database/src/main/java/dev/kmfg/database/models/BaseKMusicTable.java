package dev.kmfg.database.models;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseKMusicTable {
	@Column(name = "created_at", insertable = false, updatable = false)
	private Timestamp createdAt;
	@Column(name = "updated_at", insertable = false, updatable = true)
	protected Timestamp updatedAt;

	public Timestamp getCreatedAt() {
		return this.createdAt;
	}

	public Timestamp getUpdatedAt() {
		return this.updatedAt;
	}
}
