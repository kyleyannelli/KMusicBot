package dev.kmfg.database.repositories;

import java.util.List;
import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.tinylog.Logger;

import dev.kmfg.database.models.DiscordGuild;
import dev.kmfg.database.models.KMusicSong;
import dev.kmfg.database.models.TrackedSong;

public class TrackedSongRepo {
    private final SessionFactory sessionFactory;

    public TrackedSongRepo(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Optional<TrackedSong> findById(int id) {
        try(Session session = this.sessionFactory.openSession()) {
            return Optional.ofNullable(
                    session.find(TrackedSong.class, id)
                    );
        }
        catch(HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while finding TrackedSong by ID.");
            return Optional.empty();
        }
    }

    public Optional<TrackedSong> save(TrackedSong trackedSong) {
        try(Session session = this.sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            trackedSong = session.merge(trackedSong);
            transaction.commit();
            return Optional.ofNullable(trackedSong);
        }
        catch(HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while saving TrackedSong");
            return Optional.empty();
        }
    }

    public List<TrackedSong> findByDiscordGuild(DiscordGuild discordGuild) {
        return findByDiscordGuildId(discordGuild.getDiscordId());
    }

    public List<TrackedSong> findByDiscordGuildId(long discordId) {
        try(Session session = this.sessionFactory.openSession()) {
            return session
                .createQuery("FROM TrackedSong WHERE discordGuild.discordId = :discordGuildId", TrackedSong.class)
                .setParameter("discordGuildId", discordId)
                .getResultList();
        }
        catch(HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while finding TrackedSongs by Discord Guild id");
            return List.of();
        }
    }

    public List<TrackedSong> findByKMusicSong(KMusicSong kmusicSong) {
        return findByKMusicSongId(kmusicSong.getId());
    }

    public List<TrackedSong> findByKMusicSongId(int id) {
        try(Session session = this.sessionFactory.openSession()) {
            return session
                .createQuery("FROM TrackedSong WHERE kmusicSong.id = :kmusicSongId", TrackedSong.class)
                .setParameter("kmusicSongId", id)
                .getResultList();
        }
        catch(HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while finding TrackedSongs by KMusic Song ID");
            return List.of();
        }
    }
}
