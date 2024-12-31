package dev.kmfg.musicbot.database.repositories;

import java.util.List;
import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.tinylog.Logger;

import dev.kmfg.musicbot.database.models.DiscordGuild;
import dev.kmfg.musicbot.database.models.DiscordUser;
import dev.kmfg.musicbot.database.models.UsedCommand;

public class UsedCommandRepo {
    private final SessionFactory sessionFactory;

    public UsedCommandRepo(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Optional<UsedCommand> findById(int id) {
        try (Session session = this.sessionFactory.openSession()) {
            return Optional.ofNullable(
                    session.find(UsedCommand.class, id));
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while finding UsedCommand by ID.");
            return Optional.empty();
        }
    }

    public UsedCommand saveOrGet(UsedCommand usedCommand) {
        return this.findByAll(usedCommand).orElseGet(() -> {
            return this.save(usedCommand).get();
        });
    }

    public Optional<UsedCommand> findByAll(UsedCommand usedCommand) {
        try (Session session = this.sessionFactory.openSession()) {
            return session
                    .createQuery(
                            "FROM UsedCommand WHERE usedByDiscordUser.discordId = :discordUserId AND discordGuild.discordId = :discordGuildId AND name = :commandName",
                            UsedCommand.class)
                    .setParameter("commandName", usedCommand.getName())
                    .setParameter("discordUserId", usedCommand.usedBy().getDiscordId())
                    .setParameter("discordGuildId", usedCommand.getGuild().getDiscordId())
                    .uniqueResultOptional();
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while saving UsedCommand.");
            return Optional.empty();
        }
    }

    public Optional<UsedCommand> createOrGet(
            final String commandName,
            final DiscordGuild discordGuild,
            final DiscordUser discordUser) {
        try (Session session = this.sessionFactory.openSession()) {
            UsedCommand command = session
                    .createQuery(
                            "FROM UsedCommand WHERE usedByDiscordUser.discordId = :discordUserId AND discordGuild.discordId = :discordGuildId AND name = :commandName",
                            UsedCommand.class)
                    .setParameter("commandName", commandName)
                    .setParameter("discordUserId", discordUser.getDiscordId())
                    .setParameter("discordGuildId", discordGuild.getDiscordId())
                    .getSingleResult();
            if (command == null) {
                command = new UsedCommand(commandName, discordGuild, discordUser);
            }
            return Optional.of(command);
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while saving UsedCommand.");
            return Optional.empty();
        }
    }

    public Optional<UsedCommand> save(UsedCommand usedCommand) {
        try (Session session = this.sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            usedCommand = session.merge(usedCommand);
            transaction.commit();
            return Optional.ofNullable(usedCommand);
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while saving UsedCommand.");
            return Optional.empty();
        }
    }

    public List<UsedCommand> findByDiscordGuild(DiscordGuild discordGuild) {
        return findByDiscordGuildId(discordGuild.getDiscordId());
    }

    public List<UsedCommand> findByDiscordGuildId(long discordId) {
        try (Session session = this.sessionFactory.openSession()) {
            return session
                    .createQuery("FROM UsedCommand WHERE discordGuild.discordId = :discordGuildId", UsedCommand.class)
                    .setParameter("discordGuildId", discordId)
                    .getResultList();
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while finding UsedCommand by DiscordGuild ID");
            return List.of();
        }
    }

    public List<UsedCommand> findByDiscordUser(DiscordUser discordUser) {
        return findByDiscordUserId(discordUser.getDiscordId());
    }

    public List<UsedCommand> findByDiscordUserId(long discordId) {
        try (Session session = this.sessionFactory.openSession()) {
            return session
                    .createQuery("FROM UsedCommand WHERE usedByDiscordUser.discordId = :discordUserId",
                            UsedCommand.class)
                    .setParameter("discordUserId", discordId)
                    .getResultList();
        } catch (HibernateException hibernateException) {
            Logger.error(hibernateException, "Error occurred while finding UsedCommand by DiscordUser ID");
            return List.of();
        }
    }
}
