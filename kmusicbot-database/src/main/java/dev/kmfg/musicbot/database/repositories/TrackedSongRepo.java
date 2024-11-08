package dev.kmfg.musicbot.database.repositories;

import java.util.List;
import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.tinylog.Logger;

import dev.kmfg.musicbot.database.models.DiscordGuild;
import dev.kmfg.musicbot.database.models.KMusicSong;
import dev.kmfg.musicbot.database.models.PaginatedResponse;
import dev.kmfg.musicbot.database.models.TrackedSong;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

public class TrackedSongRepo {
    private final SessionFactory sessionFactory;

    public TrackedSongRepo(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public TrackedSong saveOrGet(TrackedSong trackedSong) {
        return this.findBySongAndGuild(trackedSong.getGuild(), trackedSong.getSong()).orElseGet(() -> {
            return this.save(trackedSong).get();
        });
    }

    public Long getPlaytimeByGuild(DiscordGuild discordGuild) {
        try (Session session = this.sessionFactory.openSession()) {
            return session
                    .createQuery(
                            "SELECT SUM(secondsPlayed) FROM TrackedSong WHERE discordGuild.discordId = :discordGuildId",
                            Long.class)
                    .setParameter("discordGuildId", discordGuild.getDiscordId())
                    .uniqueResult();
        }
    }

    public Long getSongCountByGuild(DiscordGuild discordGuild) {
        try (Session session = this.sessionFactory.openSession()) {
            return session
                    .createQuery("SELECT COUNT(*) FROM TrackedSong WHERE discordGuild.discordId = :discordGuildId",
                            Long.class)
                    .setParameter("discordGuildId", discordGuild.getDiscordId())
                    .uniqueResult();
        }
    }

    public Optional<TrackedSong> findBySongAndGuild(DiscordGuild discordGuild, KMusicSong kmusicSong) {
        try (Session session = this.sessionFactory.openSession()) {
            return session
                    .createQuery(
                            "FROM TrackedSong WHERE discordGuild.discordId = :discordGuildId AND kmusicSong.id = :kmusicSongId",
                            TrackedSong.class)
                    .setParameter("discordGuildId", discordGuild.getDiscordId())
                    .setParameter("kmusicSongId", kmusicSong.getId())
                    .uniqueResultOptional();
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while attempting to find by DiscordGuild and KMusicSong");
            return Optional.empty();
        } catch (Exception e) {
            Logger.error(e, "Error occurred while attempting to find by DiscordGuild and KMusicSong");
            return Optional.empty();
        }
    }

    public Optional<TrackedSong> findByIdAndGuildId(int id, long discordGuildId) {
        try (Session session = this.sessionFactory.openSession()) {
            return session
                    .createQuery("FROM TrackedSong WHERE discordGuild.discordId = :discordGuildId AND id = :id",
                            TrackedSong.class)
                    .setParameter(":discordGuildId", discordGuildId)
                    .setParameter(":id", id)
                    .uniqueResultOptional();
        }
    }

    public Optional<TrackedSong> findById(int id) {
        try (Session session = this.sessionFactory.openSession()) {
            return Optional.ofNullable(
                    session.find(TrackedSong.class, id));
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while finding TrackedSong by ID.");
            return Optional.empty();
        }
    }

    public Optional<TrackedSong> save(TrackedSong trackedSong) {
        try (Session session = this.sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            trackedSong = session.merge(trackedSong);
            transaction.commit();
            return Optional.ofNullable(trackedSong);
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while saving TrackedSong");
            return Optional.empty();
        }
    }

    public List<TrackedSong> findByDiscordGuild(DiscordGuild discordGuild) {
        return findByDiscordGuildId(discordGuild.getDiscordId());
    }

    public List<TrackedSong> findByDiscordGuildId(long discordId) {
        try (Session session = this.sessionFactory.openSession()) {
            return session
                    .createQuery("FROM TrackedSong WHERE discordGuild.discordId = :discordGuildId", TrackedSong.class)
                    .setParameter("discordGuildId", discordId)
                    .getResultList();
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while finding TrackedSongs by Discord Guild id");
            return List.of();
        }
    }

    public PaginatedResponse<TrackedSong> findByDiscordGuildIdAndSearchQuery(long discordId, int page, int size,
            String searchQuery) {
        try (Session session = this.sessionFactory.openSession()) {
            Query countQuery = session.createQuery(
                    "SELECT COUNT(ts) FROM TrackedSong ts " +
                            "JOIN ts.kmusicSong km " +
                            "WHERE ts.discordGuild.discordId = :discordGuildId " +
                            "AND (LOWER(km.title) LIKE :searchQuery OR LOWER(km.author) LIKE :searchQuery)",
                    Long.class);
            countQuery.setParameter("discordGuildId", discordId);
            countQuery.setParameter("searchQuery", "%" + searchQuery.toLowerCase() + "%");
            long totalItems = (long) countQuery.getSingleResult();

            List<TrackedSong> songs = session
                    .createQuery(
                            "FROM TrackedSong ts " +
                                    "JOIN FETCH ts.kmusicSong km " +
                                    "WHERE ts.discordGuild.discordId = :discordGuildId " +
                                    "AND (LOWER(km.title) LIKE :searchQuery OR LOWER(km.author) LIKE :searchQuery) " +
                                    "ORDER BY ts.secondsPlayed DESC",
                            TrackedSong.class)
                    .setParameter("discordGuildId", discordId)
                    .setParameter("searchQuery", "%" + searchQuery.toLowerCase() + "%")
                    .setFirstResult(page * size)
                    .setMaxResults(size)
                    .getResultList();

            int totalPages = (int) Math.ceil((double) totalItems / size);

            return new PaginatedResponse<>(songs, page, totalPages, totalItems, size);

        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException,
                    "Error occurred while finding TrackedSongs by Discord Guild id and search query");
            return new PaginatedResponse<>(null, 0, 0, 0L, 0);
        }
    }

    public PaginatedResponse<TrackedSong> findByDiscordGuildId(long discordId, int page, int size) {
        try (Session session = this.sessionFactory.openSession()) {
            // Query to find the total number of TrackedSong records
            Query countQuery = session.createQuery(
                    "SELECT COUNT(ts) FROM TrackedSong ts WHERE ts.discordGuild.discordId = :discordGuildId",
                    Long.class);
            countQuery.setParameter("discordGuildId", discordId);
            long totalItems = (long) countQuery.getSingleResult();

            // Query to get the paginated list of TrackedSong
            List<TrackedSong> songs = session
                    .createQuery(
                            "FROM TrackedSong WHERE discordGuild.discordId = :discordGuildId ORDER BY secondsPlayed DESC",
                            TrackedSong.class)
                    .setParameter("discordGuildId", discordId)
                    .setFirstResult(page * size)
                    .setMaxResults(size)
                    .getResultList();

            // Calculate the total number of pages
            int totalPages = (int) Math.ceil((double) totalItems / size);

            // Construct and return the paginated response
            PaginatedResponse<TrackedSong> response = new PaginatedResponse<>(songs, page, totalPages, totalItems,
                    size);

            return response;
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while finding TrackedSongs by Discord Guild id");
            return new PaginatedResponse<>(null, 0, 0, 0L, 0);
        }
    }

    public List<TrackedSong> findByKMusicSong(KMusicSong kmusicSong) {
        return findByKMusicSongId(kmusicSong.getId());
    }

    public List<TrackedSong> findByKMusicSongId(int id) {
        try (Session session = this.sessionFactory.openSession()) {
            return session
                    .createQuery("FROM TrackedSong WHERE kmusicSong.id = :kmusicSongId", TrackedSong.class)
                    .setParameter("kmusicSongId", id)
                    .getResultList();
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while finding TrackedSongs by KMusic Song ID");
            return List.of();
        }
    }
}
