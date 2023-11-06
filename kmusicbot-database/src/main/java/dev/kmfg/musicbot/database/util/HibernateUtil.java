package dev.kmfg.musicbot.database.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.tinylog.Logger;

public class HibernateUtil {
	public static SessionFactory getSessionFactory() {
		try {
			return new Configuration().configure("/hibernate.cfg.xml").buildSessionFactory();
		}
		catch(Exception e) {
			Logger.error(e, "Unable to connect to SQL server!");
			return null;
		}
	}
}
