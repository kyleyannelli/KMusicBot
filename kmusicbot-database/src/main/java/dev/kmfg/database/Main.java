package dev.kmfg.database;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class Main {
	public static void main(String[] args) {
		SessionFactory sessionFactory = new Configuration().configure("/hibernate.cfg.xml").buildSessionFactory();

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
