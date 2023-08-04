package DiscordBot.Commands;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import Helpers.MessageSender;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.tinylog.Logger;

import DiscordBot.Sessions.AudioSession;
import DiscordBot.EmbedMessage;
import DiscordBot.Sessions.SessionManager;
import Exceptions.BadAudioConnectionException;
import Exceptions.EmptyParameterException;
import Exceptions.EmptyServerException;
import Helpers.EnsuredSlashCommandInteraction;

public abstract class Command {
	protected final SessionManager sessionManager;
	protected final SlashCommandCreateEvent slashCommandEvent;
	protected final MessageSender messageSender;

	public Command(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
		this.sessionManager = sessionManager;
		this.slashCommandEvent = slashCommandEvent;

		EmbedMessage embedMessage = new EmbedMessage(this.slashCommandEvent, respondLater);
		this.messageSender = new MessageSender(embedMessage);
	}

	public abstract void execute();

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
			this.messageSender.sendEmptyParameterMessage(e.getCausalParameterName());
			return null;
		} catch (BadAudioConnectionException e) {
			Logger.warn(e.getMessage());
			this.messageSender.sendNotInServerVoiceChannelMessage();
			return null;
		} catch (EmptyServerException e) {
			Logger.warn("Server was not present in an interaction!");
			this.messageSender.sendEmptyServerMessage();
			return null;
		}
		return ensuredInteraction;
	}

}
