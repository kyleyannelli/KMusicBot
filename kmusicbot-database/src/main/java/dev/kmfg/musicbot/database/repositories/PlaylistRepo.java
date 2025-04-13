package dev.kmfg.musicbot.database.repositories;

import java.util.List;
import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.tinylog.Logger;

import dev.kmfg.musicbot.database.models.DiscordGuild;
import dev.kmfg.musicbot.database.models.Playlist;

public class PlaylistRepo {
    private final SessionFactory sessionFactory;

    public PlaylistRepo(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Optional<Playlist> findById(int id) {
        try (Session session = this.sessionFactory.openSession()) {
            return Optional.ofNullable(session.find(Playlist.class, id));
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException, "Exception occurred while opening session to find KMusicSong by ID.");
            return Optional.empty();
        } catch (Exception e) {
            Logger.error(e, "Error occurred while finding by id");
            return Optional.empty();
        }
    }

    public List<Playlist> findByGuild(DiscordGuild guild) {
        try (Session session = this.sessionFactory.openSession()) {
            return session
                    .createQuery(
                            "FROM Playlist WHERE guild.discordId = :discordGuildId",
                            Playlist.class)
                    .setParameter("discordGuildId", guild.getDiscordId())
                    .list();
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while getting playlist by all.");
            return List.of();
        }
    }

    public Optional<Playlist> findByAll(Playlist playlist) {
        try (Session session = this.sessionFactory.openSession()) {
            return session
                    .createQuery(
                            "FROM Playlist WHERE guild.discordId = :discordGuildId AND name = :playlistName",
                            Playlist.class)
                    .setParameter("discordGuildId", playlist.getGuild().getDiscordId())
                    .setParameter("playlistName", playlist.getName())
                    .uniqueResultOptional();
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while getting playlist by all.");
            return Optional.empty();
        }
    }

    public Optional<Playlist> save(Playlist playlist) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            playlist = session.merge(playlist);
            transaction.commit();
            return Optional.ofNullable(playlist);
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occured while saving SongPlaytime");
            return Optional.empty();
        }
    }

    public Playlist saveOrGet(Playlist playlist) {
        return this.findByAll(playlist).orElseGet(() -> {
            return this.save(playlist).get();
        });
    }
}
