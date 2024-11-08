package dev.kmfg.musicbot.database.repositories;

import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.tinylog.Logger;

import dev.kmfg.musicbot.database.models.KMusicSong;

public class KMusicSongRepo {
    private final SessionFactory sessionFactory;

    public KMusicSongRepo(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public KMusicSong saveOrGet(KMusicSong kmusicSong) {
        return this.findByYoutubeUrl(kmusicSong.getYoutubeUrl()).orElseGet(() -> {
            return this.save(kmusicSong).get();
        });
    }

    public Optional<KMusicSong> findById(int id) {
        try (Session session = this.sessionFactory.openSession()) {
            return Optional.ofNullable(session.find(KMusicSong.class, id));
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException, "Exception occurred while opening session to find KMusicSong by ID.");
            return Optional.empty();
        } catch (Exception e) {
            Logger.error(e, "Error occurred while finding by id");
            return Optional.empty();
        }
    }

    public Optional<KMusicSong> findByAuthor(String author) {
        try (Session session = this.sessionFactory.openSession()) {
            return Optional.ofNullable(
                    session
                            .createQuery("FROM KMusicSong WHERE author = :author", KMusicSong.class)
                            .setParameter("author", author)
                            .uniqueResult());
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException,
                    "Exception occurred while opening session to find KMusicSong by author.");
            return Optional.empty();
        }
    }

    public Optional<KMusicSong> findByTitle(String title) {
        try (Session session = this.sessionFactory.openSession()) {
            return Optional.ofNullable(
                    session
                            .createQuery("FROM KMusicSong WHERE title = :title", KMusicSong.class)
                            .setParameter("title", title)
                            .uniqueResult());
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException,
                    "Exception occurred while opening session to find KMusicSong by title.");
            return Optional.empty();
        }
    }

    public Optional<KMusicSong> findByYoutubeUrl(String youtubeUrl) {
        try (Session session = this.sessionFactory.openSession()) {
            return Optional.ofNullable(
                    session
                            .createQuery("FROM KMusicSong WHERE youtubeUrl = :youtubeUrl", KMusicSong.class)
                            .setParameter("youtubeUrl", youtubeUrl)
                            .uniqueResult());
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException,
                    "Exception occurred while opening session to find KMusicSong by YouTube URL.");
            return Optional.empty();
        }
    }

    public Optional<KMusicSong> save(KMusicSong song) {
        Session session;

        try {
            session = sessionFactory.openSession();
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException, "Exception occurred while opening session to save KMusicSong.");
            return Optional.empty();
        }

        try {
            Transaction transaction = session.beginTransaction();

            String queryString = "FROM KMusicSong WHERE youtubeUrl = :youtubeUrl";
            KMusicSong existingSong = session.createQuery(queryString, KMusicSong.class)
                    .setParameter("youtubeUrl", song.getYoutubeUrl())
                    .uniqueResult();

            if (existingSong != null) {
                existingSong.setAuthor(song.getAuthor().orElseGet(() -> {
                    return null;
                }));
                existingSong.setTitle(song.getTitle().orElseGet(() -> {
                    return null;
                }));
                song = session.merge(existingSong);
            } else {
                song = session.merge(song);
            }

            transaction.commit();
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException, "Exception occurred while saving KMusicSong.");
            return Optional.empty();
        } finally {
            session.close();
        }

        return Optional.ofNullable(song);
    }

}
