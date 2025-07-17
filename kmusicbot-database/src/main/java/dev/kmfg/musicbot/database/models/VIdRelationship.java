package dev.kmfg.musicbot.database.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "v_id_relationships",
        indexes = {
                @Index(name = "idx_v_id", columnList = "v_id"),
                @Index(name = "idx_related_v_id", columnList = "related_v_id")
        })
@IdClass(VIdRelationshipId.class)
public class VIdRelationship {

    @Id
    @Column(name = "v_id", nullable = false, length = 255)
    private String vId;

    @Id
    @Column(name = "related_v_id", nullable = false, length = 255)
    private String relatedVId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public VIdRelationship() {}

    public VIdRelationship(String vId, String relatedVId) {
        this.vId = vId;
        this.relatedVId = relatedVId;
    }

    public String getVId() {
        return vId;
    }

    public void setVId(String vId) {
        this.vId = vId;
    }

    public String getRelatedVId() {
        return relatedVId;
    }

    public void setRelatedVId(String relatedVId) {
        this.relatedVId = relatedVId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VIdRelationship that = (VIdRelationship) o;
        return Objects.equals(vId, that.vId) &&
                Objects.equals(relatedVId, that.relatedVId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vId, relatedVId);
    }

    @Override
    public String toString() {
        return "VIdRelationship{" +
                "vId='" + vId + '\'' +
                ", relatedVId='" + relatedVId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}