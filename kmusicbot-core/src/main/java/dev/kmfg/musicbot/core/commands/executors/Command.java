package dev.kmfg.musicbot.core.commands.executors;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import dev.kmfg.musicbot.core.exceptions.BadAudioConnectionException;
import dev.kmfg.musicbot.core.exceptions.EmptyParameterException;
import dev.kmfg.musicbot.core.exceptions.EmptyServerException;
import dev.kmfg.musicbot.core.util.slashcommands.EnsuredSlashCommandInteraction;
import dev.kmfg.musicbot.core.sessions.AudioSession;
import dev.kmfg.musicbot.core.sessions.SessionManager;
import dev.kmfg.musicbot.core.util.messages.MessageSender;

import org.hibernate.SessionFactory;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.tinylog.Logger;

import dev.kmfg.musicbot.core.util.messages.EmbedMessage;
import dev.kmfg.musicbot.database.models.DiscordGuild;
import dev.kmfg.musicbot.database.models.DiscordUser;
import dev.kmfg.musicbot.database.models.UsedCommand;
import dev.kmfg.musicbot.database.repositories.UsedCommandRepo;

public abstract class Command {
    private final ExecutorService executorService;

    protected final SessionManager sessionManager;
    protected final SlashCommandCreateEvent slashCommandEvent;
    protected final MessageSender messageSender;
    protected final DiscordUser discordUser;
    protected final DiscordGuild discordGuild;

    public Command(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent,
            ExecutorService executorService) {
        this.executorService = executorService;

        this.sessionManager = sessionManager;

        this.slashCommandEvent = slashCommandEvent;

        EmbedMessage embedMessage = new EmbedMessage(this.slashCommandEvent.getSlashCommandInteraction().getUser(),
                getRespondLater());
        this.messageSender = new MessageSender(embedMessage);

        this.discordUser = this.generateDiscordUser();
        final Optional<DiscordGuild> discordGuildOpt = this.generateDiscordGuild();
        if (discordGuildOpt.isPresent()) {
            this.discordGuild = discordGuildOpt.get();
        } else {
            Logger.error(
                    "Failed to get server from used command. Commands should not be able to be used outside of servers! Please contact the developer.");
            this.discordGuild = null;
        }
    }

    public Command() {
        this.executorService = null;
        this.slashCommandEvent = null;
        this.messageSender = null;
        this.discordUser = null;
        this.sessionManager = null;
        this.discordGuild = null;
    }

    /**
     * Allows the respondLater Object to be accessed for message responses
     */
    public CompletableFuture<InteractionOriginalResponseUpdater> getRespondLaterFromEmbed() {
        return this.messageSender.getEmbedMessage().getRespondLater();
    }

    /**
     * Generates the DiscordUser from interaction (statistics tracking)
     */
    public DiscordUser generateDiscordUser() {
        User user = this.slashCommandEvent.getInteraction().getUser();
        return new DiscordUser(user.getId(), user.getDiscriminatedName());
    }

    /**
     * Generates the DiscordGuild from interaction (statistics tracking)
     * Due to the nature of a {@link org.javacord.api.interaction.SlashCommand} it
     * is possible this doesn't have a guild.
     */
    public Optional<DiscordGuild> generateDiscordGuild() {
        Optional<Server> serverOpt = this.slashCommandEvent.getInteraction().getServer();
        if (serverOpt.isEmpty()) {
            return Optional.empty();
        }
        final Server server = serverOpt.get();
        final DiscordGuild discordGuild = new DiscordGuild(server.getId());
        return Optional.of(discordGuild);
    }

    /**
     * Registers the command with the Discord API
     * 
     * @param discordApi, the discordApi object to register with
     */
    public abstract void register(DiscordApi discordApi);

    /**
     * Gets the command name
     */
    public abstract String getCommandName();

    /**
     * Gets the description of the command
     */
    public abstract String getCommandDescription();

    /**
     * This method should entirely handle the SlashCommandCreateEvent
     * This means responding to it with an EmbedMessage and handling related
     * effects.
     * EnsuredInteraction may appear with a null check frequently, however, there is
     * no reason to fully implement it as a constant.
     * Some commands do not require the EnsuredInteraction
     */
    public void execute() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                markCommandUsed();
            }
        };
        this.executorService.submit(runnable);
    }

    /**
     * pulls the respond later future out of the event
     */
    public CompletableFuture<InteractionOriginalResponseUpdater> getRespondLater() {
        return this.slashCommandEvent.getInteraction().respondLater();
    }

    public AudioSession createOrGetAudioSession(long serverId) {
        AudioSession audioSession = this.sessionManager.getAudioSession(serverId);

        if (audioSession == null) {
            audioSession = this.sessionManager.createAudioSession(serverId);
        }

        return audioSession;
    }

    /**
     * Mark that a command has been used in the database.
     */
    protected void markCommandUsed() {
        String commandName = this.getCommandName();
        if (commandName == null || commandName.isBlank()) {
            commandName = this.getClass().toString();
            Logger.warn(
                    "Command Class {} did not properly override the getCommandName method. Therefore, the class name was used. This is not intended behavior. Please contact @806350925723205642 on Discord or open an issue.",
                    commandName);
        }

        final SessionFactory sFactory = this.sessionManager.getSessionFactory();
        final UsedCommandRepo commandRepo = new UsedCommandRepo(sFactory);
        UsedCommand usedCommand = commandRepo.saveOrGet(new UsedCommand(commandName, discordGuild, discordUser));
        usedCommand.incTimesUsed();
        usedCommand.refreshUpdatedAt();
        usedCommand = commandRepo.save(usedCommand).get();
    }

    protected EnsuredSlashCommandInteraction getEnsuredInteraction(ArrayList<String> requiredParams) {
        EnsuredSlashCommandInteraction ensuredInteraction;
        try {
            ensuredInteraction = new EnsuredSlashCommandInteraction(this, slashCommandEvent, requiredParams);
        } catch (EmptyParameterException e) {
            Logger.warn(e.getCausalParameterName() + " was not in interaction for command, sending discord message...");
            this.messageSender.sendEmptyParameterEmbed(e.getCausalParameterName());
            return null;
        } catch (BadAudioConnectionException e) {
            Logger.warn(e.getMessage());
            this.messageSender.sendNotInServerVoiceChannelEmbed();
            return null;
        } catch (EmptyServerException e) {
            Logger.warn("Server was not present in an interaction!");
            this.messageSender.sendEmptyServerEmbed();
            return null;
        }
        return ensuredInteraction;
    }
}
