package dev.kmfg.database.repositories;

import java.util.List;
import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.tinylog.Logger;

import dev.kmfg.database.models.DiscordUser;
import dev.kmfg.database.models.SongPlaytime;
import dev.kmfg.database.models.TrackedSong;

public class SongPlaytimeRepo {
    private final SessionFactory sessionFactory;

    public SongPlaytimeRepo(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Optional<SongPlaytime> findById(int id) {
        try(Session session = this.sessionFactory.openSession()) {
            return Optional.ofNullable(
                    session.find(SongPlaytime.class, id)
                    );
        }
        catch(HibernateException hibernateException) {
             Logger.error(hibernateException, "Error occurred while finding SongPlaytime by ID.");
             return Optional.empty();
        }
    }

    public List<SongPlaytime> findByDiscordUser(DiscordUser discordUser) {
        return findByDiscordUserId(discordUser.getDiscordId());
    }

    public List<SongPlaytime> findByDiscordUserId(long id) {
        try(Session session = this.sessionFactory.openSession()) {
            return session
                .createQuery("FROM SongPlaytime WHERE listeningDiscordUser.discordId = :discordUserId", SongPlaytime.class)
                .setParameter("discordUserId", id)
                .getResultList();
        }
        catch(HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while finding SongPlaytimes by discord user id.");
            return List.of();
        }
    }

    public List<SongPlaytime> findByTrackedSong(TrackedSong trackedSong) {
        return findByTrackedSongId(trackedSong.getId());
    }

    public List<SongPlaytime> findByTrackedSongId(int id) {
        try(Session session = this.sessionFactory.openSession()) {
            return session
                .createQuery("FROM SongPlaytime WHERE trackedSong.id = :trackedSongId", SongPlaytime.class)
                .setParameter("trackedSongId", id)
                .getResultList();
        }
        catch(HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while finding SongPlaytimes by tracked song id.");
            return List.of();
        }
    }
}
