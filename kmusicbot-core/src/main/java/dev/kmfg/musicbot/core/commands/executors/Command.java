package dev.kmfg.musicbot.core.commands.executors;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import dev.kmfg.musicbot.core.exceptions.BadAudioConnectionException;
import dev.kmfg.musicbot.core.exceptions.EmptyParameterException;
import dev.kmfg.musicbot.core.exceptions.EmptyServerException;
import dev.kmfg.musicbot.core.util.slashcommands.EnsuredSlashCommandInteraction;
import dev.kmfg.musicbot.core.sessions.AudioSession;
import dev.kmfg.musicbot.core.sessions.SessionManager;
import dev.kmfg.musicbot.core.util.messages.MessageSender;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.tinylog.Logger;

import dev.kmfg.musicbot.core.util.messages.EmbedMessage;
import dev.kmfg.musicbot.database.models.DiscordUser;

public abstract class Command {
	protected final SessionManager sessionManager;
	protected final SlashCommandCreateEvent slashCommandEvent;
	protected final MessageSender messageSender;
	protected final DiscordUser discordUser;

	public Command(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent) {
		this.sessionManager = sessionManager;

		this.slashCommandEvent = slashCommandEvent;

		EmbedMessage embedMessage = new EmbedMessage(this.slashCommandEvent.getSlashCommandInteraction().getUser(), getRespondLater());
		this.messageSender = new MessageSender(embedMessage);

		this.discordUser = this.generateDiscordUser();

		// finally register the command
		this.register(this.sessionManager.getDiscordApi());
	}

	/**
	 * Generates the DiscordUser from interaction (statistics tracking)
	 */
	public DiscordUser generateDiscordUser() {
		User user = this.slashCommandEvent.getInteraction().getUser();
		return new DiscordUser(user.getId(), user.getDiscriminatedName());
	}

	/**
	 * Registers the command with the Discord API
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
	 * This means responding to it with an EmbedMessage and handling related effects.
	 * EnsuredInteraction may appear with a null check frequently, however, there is no reason to fully implement it as a constant.
	 * Some commands do not require the EnsuredInteraction
	 */
	public abstract void execute();

	/**
	 * pulls the respond later future out of the event
	 */
	public CompletableFuture<InteractionOriginalResponseUpdater> getRespondLater() {
		return this.slashCommandEvent.getInteraction().respondLater();
	}

	public AudioSession createOrGetAudioSession(long serverId){
		AudioSession audioSession = this.sessionManager.getAudioSession(serverId);

		if(audioSession == null) {
			audioSession = this.sessionManager.createAudioSession(serverId);
		}

		return audioSession;
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
