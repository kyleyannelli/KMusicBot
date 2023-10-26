package dev.kmfg.database.repositories;

import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.tinylog.Logger;

import dev.kmfg.database.models.KMusicSong;
import dev.kmfg.database.repositories.interfaces.KMusicSongRepository;

public class HibernateKMusicSongRepository implements KMusicSongRepository {
	private final SessionFactory sessionFactory;

	public HibernateKMusicSongRepository(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public Optional<KMusicSong> findById(long id) {
		Session session;

		try {
			session = this.sessionFactory.openSession();
		}
		catch(HibernateException hibernateException) {
			Logger.error(hibernateException, "Exception occured while opening session to find KMusicSong by ID.");
			return Optional.empty();
		}

		Optional<KMusicSong> kmusicSong = Optional.ofNullable(session.find(KMusicSong.class, id));
		session.close();
		return kmusicSong;
	}

	@Override
	public Optional<KMusicSong> findByYoutubeUrl(String youtubeUrl) {
		Session session;

		try {
			session = this.sessionFactory.openSession();
		}
		catch(HibernateException hibernateException) {
			Logger.error(hibernateException, "Exception occured while opening session to find KMusicSong by YouTube URL.");
			return Optional.empty();
		}

		Optional<KMusicSong> kmusicSong = Optional.ofNullable(
				session
				.createQuery("FROM KMusicSong WHERE youtube_url = :youtubeUrl", KMusicSong.class)
				.setParameter("youtubeUrl", youtubeUrl)
				.uniqueResult()
				);
		session.close();
		return kmusicSong;
	}

	@Override
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
