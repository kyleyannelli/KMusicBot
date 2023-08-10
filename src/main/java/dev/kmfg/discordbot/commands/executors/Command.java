package dev.kmfg.discordbot.commands.executors;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import dev.kmfg.helpers.messages.MessageSender;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.tinylog.Logger;

import dev.kmfg.sessions.AudioSession;
import dev.kmfg.helpers.messages.EmbedMessage;
import dev.kmfg.sessions.SessionManager;
import dev.kmfg.exceptions.BadAudioConnectionException;
import dev.kmfg.exceptions.EmptyParameterException;
import dev.kmfg.exceptions.EmptyServerException;
import dev.kmfg.helpers.slashcommands.EnsuredSlashCommandInteraction;

public abstract class Command {
	protected final SessionManager sessionManager;
	protected final SlashCommandCreateEvent slashCommandEvent;
	protected final MessageSender messageSender;

	public Command(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent) {
		this.sessionManager = sessionManager;

		this.slashCommandEvent = slashCommandEvent;

		EmbedMessage embedMessage = new EmbedMessage(this.slashCommandEvent.getSlashCommandInteraction().getUser(), getRespondLater());
		this.messageSender = new MessageSender(embedMessage);

		// finally register the command
		this.register(this.sessionManager.getDiscordApi());
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
