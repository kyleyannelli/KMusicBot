package dev.kmfg.musicbot.database.repositories;

import dev.kmfg.musicbot.database.models.DiscordUser;
import dev.kmfg.musicbot.database.models.SongInitialization;
import dev.kmfg.musicbot.database.models.TrackedSong;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.tinylog.Logger;

import java.util.List;
import java.util.Optional;

public class SongInitializationRepo {
    private final SessionFactory sessionFactory;

    public SongInitializationRepo(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public SongInitialization saveOrGet(SongInitialization songInitialization) {
        return this.findByTrackedSongAndDiscordUser(songInitialization).orElseGet(() -> {
            return this.save(songInitialization).get();
        });
    }

    public long getTotalUserInits(long discordUserId) {
        try(Session session = this.sessionFactory.openSession()) {
            return session
                .createQuery("SELECT SUM(timesInitialized) FROM SongInitialization WHERE initializingDiscordUser.discordId = :discordId", Long.class)
                .setParameter("discordId", discordUserId)
                .uniqueResult();
        }
    }

    public long getTotalUserInits(long discordUserId, long discordGuildId) {
        try(Session session = this.sessionFactory.openSession()) {
            return session
                .createQuery("SELECT SUM(timesInitialized) FROM SongInitialization WHERE initializingDiscordUser.discordId = :discordId AND trackedSong.discordGuild.discordId = :discordGuildId", Long.class)
                .setParameter("discordId", discordUserId)
                .setParameter("discordGuildId", discordGuildId)
                .uniqueResult();
        }
    }

    public Optional<SongInitialization> findByTrackedSongAndDiscordUser(SongInitialization songInitialization) {
        return this.findByTrackedSongAndDiscordUser(songInitialization.getTrackedSong(), songInitialization.getInitDiscordUser());
    }

    public Optional<SongInitialization> findByTrackedSongAndDiscordUser(TrackedSong trackedSong, DiscordUser discordUser) {
        try(Session session = this.sessionFactory.openSession()) {
            return Optional.ofNullable(session
                    .createQuery("FROM SongInitialization WHERE trackedSong.id = :trackedSongId AND initializingDiscordUser.discordId = :initializingDiscordUserId", SongInitialization.class)
                    .setParameter("trackedSongId", trackedSong.getId())
                    .setParameter("initializingDiscordUserId", discordUser.getDiscordId())
                    .getSingleResult());
        }
        catch(HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while attempting to find SongInitialization by TrackedSong and InitDiscordUser");
            return Optional.empty();
        }
        catch(Exception e) {
            Logger.error(e, "Error occurred while finding by TrackedSong and DiscordUser");
            return Optional.empty();
        }
    }

    public Optional<SongInitialization> findById(int id) {
        try(Session session = this.sessionFactory.openSession()) {
            return Optional.ofNullable(session.find(SongInitialization.class, id));
        }
        catch(HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while finding SongInitialization by id");
            return Optional.empty();
        }
    }

    public List<SongInitialization> findByDiscordUser(DiscordUser discordUser) {
        return findByDiscordUserId(discordUser.getDiscordId());
    }

    public List<SongInitialization> findByDiscordUserId(long id) {
        try(Session session = this.sessionFactory.openSession()) {
            return session
                .createQuery("FROM SongInitialization WHERE initializingDiscordUser.id = :discordUserId", SongInitialization.class)
                .setParameter("discordUserId", id)
                .getResultList();
        }
        catch(HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while finding SongInitialization by Discord User ID");
            return List.of();
        }
    }

    public List<SongInitialization> findByTrackedSong(TrackedSong trackedSong) {
        return findByTrackedSongId(trackedSong.getId());
    }

    public List<SongInitialization> findByTrackedSongId(int id) {
        try(Session session = this.sessionFactory.openSession()) {
            return session
                .createQuery("FROM SongInitialization WHERE trackedSong.id = :trackedSongId", SongInitialization.class)
                .setParameter("trackedSongId", id)
                .getResultList();
        }
        catch(HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while finding SongInitializations by tracked song ids.");
            return List.of();
        }
    }

    public Optional<SongInitialization> save(SongInitialization songInitialization) {
        try(Session session = this.sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            songInitialization = session.merge(songInitialization);
            transaction.commit();
            return Optional.ofNullable(songInitialization);
        }
        catch(HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred when trying to save SongInitialization.");
            return Optional.empty();
        }
    }
}
