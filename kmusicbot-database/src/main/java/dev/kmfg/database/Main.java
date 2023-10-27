package dev.kmfg.database;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import dev.kmfg.database.models.DiscordUser;
import dev.kmfg.database.repositories.DiscordUserRepo;

public class Main {
	public static void main(String[] args) {
		SessionFactory sessionFactory = new Configuration().configure("/hibernate.cfg.xml").buildSessionFactory();

		DiscordUserRepo discordUserRepo = new DiscordUserRepo(sessionFactory);
		DiscordUser discordUser = new DiscordUser(806350925723205642L, "chet#0000");
		discordUserRepo.save(discordUser);

		Session session = sessionFactory.openSession();

		try {
			session.beginTransaction();

			System.out.println("connected");

			session.getTransaction().commit();
		}
		catch(Exception e) {
			e.printStackTrace();
			if(session.getTransaction() != null) {
				session.getTransaction().rollback();
			}
		}
		finally {
			session.close();
			sessionFactory.close();
		}
	}
}
