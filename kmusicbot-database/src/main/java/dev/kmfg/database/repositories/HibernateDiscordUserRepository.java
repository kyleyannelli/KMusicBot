package dev.kmfg.database.repositories;

import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.tinylog.Logger;

import dev.kmfg.database.models.DiscordUser;
import dev.kmfg.database.repositories.interfaces.DiscordUserRepository;

public class HibernateDiscordUserRepository implements DiscordUserRepository {
	private final SessionFactory sessionFactory;

	public HibernateDiscordUserRepository(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public Optional<DiscordUser> findByDiscordId(long discordId) {
		Session session;

		try {
			session = sessionFactory.openSession();
		}
		catch(HibernateException hibernateException) {
			Logger.error(hibernateException, "Exception occured while opening session to find DiscordUser by id.");
			return Optional.empty();
		}

		Optional<DiscordUser> discordUser = Optional.ofNullable(session.get(DiscordUser.class, discordId));
		session.close();
		return discordUser;
	}

	@Override
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
				.createQuery("FROM DiscordUser WHERE discord_username = :discordUsername", DiscordUser.class)
				.setParameter("discordUsername", discordUsername)
				.uniqueResult()
				);

		session.close();
		return discordUser;
	}

	@Override
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
