package dev.kmfg.database.repositories;

import dev.kmfg.database.models.DiscordGuild;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.tinylog.Logger;

import java.util.Optional;

public class DiscordGuildRepo {
    private final SessionFactory sessionFactory;

    public DiscordGuildRepo(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Optional<DiscordGuild> findByDiscordId(long discordId) {
        try(Session session = sessionFactory.openSession()) {
            return Optional.ofNullable(session.get(DiscordGuild.class, discordId));
        }
        catch(HibernateException hibernateException) {
            Logger.error(hibernateException, "Exception occurred while opening session to find DiscordGuild by id.");
            return Optional.empty();
        }
    }

    public DiscordGuild saveOrGet(DiscordGuild discordGuild) {
        return this.findByDiscordId(discordGuild.getDiscordId()).orElseGet(() -> {
            return this.save(discordGuild).get();
        });
    }

    public Optional<DiscordGuild> save(DiscordGuild discordGuild) {
        try(Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            discordGuild = session.merge(discordGuild);
            transaction.commit();
            return Optional.ofNullable(discordGuild);
        }
        catch(HibernateException hibernateException) {
            Logger.error(hibernateException, "Exception occurred while opening session to save DiscordGuild.");
            return Optional.empty();
        }
    }
}
