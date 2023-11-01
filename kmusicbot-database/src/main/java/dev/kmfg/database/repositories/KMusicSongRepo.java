package dev.kmfg.database.repositories;

import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.tinylog.Logger;

import dev.kmfg.database.models.KMusicSong;

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
		try(Session session = this.sessionFactory.openSession()) {
			return Optional.ofNullable(session.find(KMusicSong.class, id));
		}
		catch(HibernateException hibernateException) {
			Logger.error(hibernateException, "Exception occurred while opening session to find KMusicSong by ID.");
			return Optional.empty();
		}
	}

	public Optional<KMusicSong> findByYoutubeUrl(String youtubeUrl) {
		try(Session session = this.sessionFactory.openSession()) {
			return Optional.ofNullable(
					session
							.createQuery("FROM KMusicSong WHERE youtubeUrl = :youtubeUrl", KMusicSong.class)
							.setParameter("youtubeUrl", youtubeUrl)
							.uniqueResult()
			);
		}
		catch(HibernateException hibernateException) {
			Logger.error(hibernateException, "Exception occurred while opening session to find KMusicSong by YouTube URL.");
			return Optional.empty();
		}
	}

	public Optional<KMusicSong> save(KMusicSong song) {
		Session session;

		try {
			session = sessionFactory.openSession();
		}
		catch(HibernateException hibernateException) {
			Logger.error(hibernateException, "Exception occured while opening session to save KMusicSong.");
			return Optional.empty();
		}

		try {
			Transaction transaction = session.beginTransaction();
			song = session.merge(song);
			transaction.commit();
		}
		catch(HibernateException hibernateException) {
			Logger.error(hibernateException, "Exception occured while saving KMusicSong.");
			return Optional.empty();
		}

		session.close();
		return Optional.ofNullable(song);
	}

}
