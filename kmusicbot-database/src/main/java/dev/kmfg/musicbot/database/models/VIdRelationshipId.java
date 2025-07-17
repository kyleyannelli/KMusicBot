package dev.kmfg.musicbot.database.models;

import java.io.Serializable;
import java.util.Objects;

public class VIdRelationshipId implements Serializable {

    private String vId;
    private String relatedVId;

    public VIdRelationshipId() {}

    public VIdRelationshipId(String vId, String relatedVId) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VIdRelationshipId that = (VIdRelationshipId) o;
        return Objects.equals(vId, that.vId) &&
                Objects.equals(relatedVId, that.relatedVId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vId, relatedVId);
    }
}