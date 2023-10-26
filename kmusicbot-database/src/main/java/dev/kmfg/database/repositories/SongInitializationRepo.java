package dev.kmfg.database.repositories;

import dev.kmfg.database.models.DiscordUser;
import dev.kmfg.database.models.SongInitialization;
import dev.kmfg.database.models.TrackedSong;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SongInitializationRepo {
    private final SessionFactory sessionFactory;

    public SongInitializationRepo(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
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
