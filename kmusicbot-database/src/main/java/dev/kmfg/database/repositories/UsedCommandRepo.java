package dev.kmfg.database.repositories;

import java.util.List;
import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.tinylog.Logger;

import dev.kmfg.database.models.DiscordGuild;
import dev.kmfg.database.models.DiscordUser;
import dev.kmfg.database.models.UsedCommand;

public class UsedCommandRepo {
    private final SessionFactory sessionFactory;

    public UsedCommandRepo(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Optional<UsedCommand> findById(int id) {
        try(Session session = this.sessionFactory.openSession()) {
            return Optional.ofNullable(
                    session.find(UsedCommand.class, id)
                    );
        }
        catch(HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while finding UsedCommand by ID.");
            return Optional.empty();
        }
    }

    public Optional<UsedCommand> save(UsedCommand usedCommand) {
        try(Session session = this.sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            usedCommand = session.merge(usedCommand);
            transaction.commit();
            return Optional.ofNullable(usedCommand);
        }
        catch(HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while saving UsedCommand.");
            return Optional.empty();
        }
    }

    public List<UsedCommand> findByDiscordGuild(DiscordGuild discordGuild) {
        return findByDiscordGuildId(discordGuild.getDiscordId());
    }

    public List<UsedCommand> findByDiscordGuildId(long discordId) {
        try(Session session = this.sessionFactory.openSession()) {
            return session
                .createQuery("FROM UsedCommand WHERE discordGuild.discordId = :disocrdGuildId", UsedCommand.class)
                .setParameter("discordGuildId", discordId)
                .getResultList();
        }
        catch(HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while finding UsedCommand by DiscordGuild ID");
            return List.of();
        }
    }

    public List<UsedCommand> findByDiscordUser(DiscordUser discordUser) {
        return findByDiscordUserId(discordUser.getDiscordId());
    }

    public List<UsedCommand> findByDiscordUserId(long discordId) {
        try(Session session = this.sessionFactory.openSession()) {
            return session
                .createQuery("FROM UsedCommand WHERE usedByDiscordUser.discordId = :discordUserId", UsedCommand.class)
                .setParameter("discordUserId", discordId)
                .getResultList();
        }
        catch(HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while finding UsedCommand by DiscordUser ID");
            return List.of();
        }
    }
}
