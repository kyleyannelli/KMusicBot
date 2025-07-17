package dev.kmfg.musicbot.database.repositories;

import dev.kmfg.musicbot.database.models.VIdRelationship;
import dev.kmfg.musicbot.database.models.VIdRelationshipId;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VIdRelationshipRepo {

    private final SessionFactory sessionFactory;

    public VIdRelationshipRepo(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void save(VIdRelationship relationship) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.createNativeQuery(
                            "INSERT IGNORE INTO v_id_relationships (v_id, related_v_id) VALUES (:vId, :relatedVId)"
                    )
                    .setParameter("vId", relationship.getVId())
                    .setParameter("relatedVId", relationship.getRelatedVId())
                    .executeUpdate();
            transaction.commit();
        } catch (ConstraintViolationException e) {
            if (transaction != null) transaction.rollback();
            Logger.warn("Attempted to persist duplicate relationship: {}", relationship);
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            Logger.error(e, "Failed to persist relationship: {}", relationship);
        }
    }

    public Optional<VIdRelationship> findById(VIdRelationshipId id) {
        try (Session session = sessionFactory.openSession()) {
            return Optional.ofNullable(session.find(VIdRelationship.class, id));
        } catch (Exception e) {
            Logger.error(e, "Failed to find relationship with ID: {}", id);
            return Optional.empty();
        }
    }

    public List<VIdRelationship> findByVId(String vId) {
        try (Session session = sessionFactory.openSession()) {
            Query<VIdRelationship> query = session.createQuery(
                    "FROM VIdRelationship WHERE vId = :vId", VIdRelationship.class);
            query.setParameter("vId", vId);
            return query.getResultList();
        } catch (Exception e) {
            Logger.error(e, "Failed to find relationships by vId: {}", vId);
            return List.of();
        }
    }

    public List<VIdRelationship> findByRelatedVId(String relatedVId) {
        try (Session session = sessionFactory.openSession()) {
            Query<VIdRelationship> query = session.createQuery(
                    "FROM VIdRelationship WHERE relatedVId = :relatedVId", VIdRelationship.class);
            query.setParameter("relatedVId", relatedVId);
            return query.getResultList();
        } catch (Exception e) {
            Logger.error(e, "Failed to find relationships by relatedVId: {}", relatedVId);
            return List.of();
        }
    }

    public boolean exists(String vId, String relatedVId) {
        try (Session session = sessionFactory.openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(*) FROM VIdRelationship WHERE vId = :vId AND relatedVId = :relatedVId", Long.class);
            query.setParameter("vId", vId);
            query.setParameter("relatedVId", relatedVId);
            return query.getSingleResult() > 0;
        } catch (Exception e) {
            Logger.error(e, "Failed to check existence for vId {} and relatedVId {}", vId, relatedVId);
            return false;
        }
    }

    public void delete(VIdRelationshipId id) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            VIdRelationship relationship = session.find(VIdRelationship.class, id);
            if (relationship != null) {
                session.remove(relationship);
            } else {
                Logger.warn("No relationship found to delete with ID: {}", id);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            Logger.error(e, "Failed to delete relationship with ID: {}", id);
        }
    }

    public List<VIdRelationship> getTopXRelationships(String sourceVId, int limit) {
        List<VIdRelationship> relationships = new ArrayList<>();
        try (Session session = sessionFactory.openSession()) {
            NativeQuery<Object[]> query = session.createNativeQuery("CALL GetTopXRelationships(:sourceVId, :limit)");
            query.setParameter("sourceVId", sourceVId);
            query.setParameter("limit", limit);

            List<Object[]> results = query.list();
            for (Object[] row : results) {
                if (row.length >= 2 && row[0] instanceof String && row[1] instanceof String) {
                    relationships.add(new VIdRelationship((String) row[0], (String) row[1]));
                } else {
                    Logger.warn("Unexpected result row format: {}", (Object) row);
                }
            }
        } catch (Exception e) {
            Logger.error(e, "Failed to fetch top {} relationships for vId: {}", limit, sourceVId);
        }

        return relationships;
    }
}
