package dev.kmfg.database.repositories;

import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.tinylog.Logger;

import dev.kmfg.database.models.DiscordUser;

public class DiscordUserRepo {
	private final SessionFactory sessionFactory;

	public DiscordUserRepo(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public DiscordUser saveOrGet(DiscordUser discordUser) {
        return this.findByDiscordId(discordUser.getDiscordId()).orElseGet(() -> {
            return this.save(discordUser).get();
        });
	}

	public Optional<DiscordUser> findByDiscordId(long discordId) {
		try(Session session = this.sessionFactory.openSession()) {
			return Optional.ofNullable(session.get(DiscordUser.class, discordId));
		}
		catch(HibernateException hibernateException) {
			Logger.error(hibernateException, "Exception occurred while opening session to find DiscordUser by id.");
			return Optional.empty();
		}
		catch(Exception e) {
			Logger.error(e, "Error occurred while finding by discord id");
			return Optional.empty();
		}
	}

	public Optional<DiscordUser> findByDiscordUsername(String discordUsername) {
		Session session;

		try {
			session = sessionFactory.openSession();
		}
		catch(HibernateException hibernateException) {
			Logger.error(hibernateException, "Exception occured while opening session to find DiscordUser by username.");
			return Optional.empty();
		}

		Optional<DiscordUser> discordUser = Optional.ofNullable(
				session
				.createQuery("FROM DiscordUser WHERE username = :discordUsername", DiscordUser.class)
				.setParameter("discordUsername", discordUsername)
				.uniqueResult()
				);

		session.close();
		return discordUser;
	}

	public Optional<DiscordUser> save(DiscordUser discordUser) {
		Session session;

		try {
			session = sessionFactory.openSession();
		}
		catch(HibernateException hibernateException) {
			Logger.error(hibernateException, "Exception occured while opening session to save DiscordUser.");
			return Optional.empty();
		}

		try {
			Transaction transaction = session.beginTransaction();
			discordUser = session.merge(discordUser);
			transaction.commit();
		}
		catch(HibernateException hibernateException) {
			Logger.error(hibernateException, "Exception occured while saving DiscordUser.");
			return Optional.empty();
		}

		session.close();
		return Optional.ofNullable(discordUser);
	}
}
