package DiscordBot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import Exceptions.BadAudioConnectionException;
import Exceptions.EmptyParameterException;
import Exceptions.EmptyServerException;
import Helpers.EnsuredSlashCommandInteraction;
import Lavaplayer.LavaSource;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.tinylog.Logger;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

import Helpers.QueueResult;
import SongRecommender.RecommenderProcessor;
import se.michaelthelin.spotify.SpotifyApi;

import javax.swing.text.html.Option;

public class Commands {
	private static final String PLAY_COMMAND_NAME = "play";
	private static final String INVITE_COMMAND_NAME = "invite";

	private final DiscordApi discordApi;
	private final SpotifyApi spotifyApi;
	private final RecommenderProcessor recommenderProcessor;
	private final AudioPlayerManager audioPlayerManager;

	public final ConcurrentHashMap<Long, AudioSession> audioSessions;

	public Commands(DiscordApi discordApi, SpotifyApi spotifyApi, RecommenderProcessor recommenderProcessor, AudioPlayerManager audioPlayerManager) {
		this.discordApi = discordApi;
		this.spotifyApi = spotifyApi;
		this.recommenderProcessor = recommenderProcessor;
		this.audioPlayerManager = audioPlayerManager;
		this.audioSessions = new ConcurrentHashMap<>();
	}

	public void createAndListenForGlobalCommands() {
		createCommands();
		listenForCommands();
	}

	public AudioSession createAudioSession(long serverId) {
		AudioSession audioSession = new AudioSession(this.recommenderProcessor, serverId);
		LavaSource lavaSource = new LavaSource(discordApi, spotifyApi, audioPlayerManager, audioSession.getSessionId());
		audioSession.setLavaSource(lavaSource);
		return audioSession;
	}

	private void createCommands() {
		createPlayCommand();
		createInviteCommand();
	}

	private void createInviteCommand() {
		SlashCommand.with(INVITE_COMMAND_NAME, "Get an invite link for the bot").createGlobal(discordApi);
	}

	private void handleInviteCommand(SlashCommandCreateEvent slashCommandEvent, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
		String inviteLink = discordApi.createBotInvite(Permissions.fromBitmask(36700160));
		EmbedMessage embedMessage = new EmbedMessage(slashCommandEvent.getSlashCommandInteraction(), respondLater);
		embedMessage.setTitle("Invite Link");
		embedMessage.setContent(inviteLink);
		embedMessage.send();
	}

	private void createPlayCommand() {
		SlashCommand.with(PLAY_COMMAND_NAME, "Play a song",
				// create option(s)
				Collections.singletonList(
					// create option /play <song>
					SlashCommandOption.create(SlashCommandOptionType.STRING, "song", "The song to play", true)
					))
			.createGlobal(discordApi).join();
	}

	private void handlePlayCommand(SlashCommandCreateEvent slashCommandEvent, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
		// message to respond to interaction
		EmbedMessage embedMessage = new EmbedMessage(slashCommandEvent.getSlashCommandInteraction(), respondLater);

		// begin ensured interaction setup
		String songParameter = "song";
		ArrayList<String> requiredParameters = new ArrayList<>();
		requiredParameters.add(songParameter);

		Optional<EnsuredSlashCommandInteraction> ensuredSlashCommandInteraction = getEnsuredInteraction(requiredParameters, slashCommandEvent, embedMessage);
		// Above method will handle sending messages, stop execution here if we don't get an EnsuredInteraction.
		if(ensuredSlashCommandInteraction.isEmpty()) return;
		EnsuredSlashCommandInteraction ensuredInteraction = ensuredSlashCommandInteraction.get();

		QueueResult queueResult = ensuredInteraction
				.getAudioSession()
				.queueSearchQuery(ensuredInteraction.getParameterValue(songParameter));

		sendQueueResultEmbed(embedMessage, queueResult);
	}

	private Optional<EnsuredSlashCommandInteraction> getEnsuredInteraction(ArrayList<String> requiredParams, SlashCommandCreateEvent slashCommandCreateEvent, EmbedMessage embedMessage) {
		EnsuredSlashCommandInteraction ensuredInteraction;
		try {
			ensuredInteraction = new EnsuredSlashCommandInteraction(this, slashCommandCreateEvent, requiredParams);
		} catch (EmptyParameterException e) {
			Logger.warn(e.getCausalParameterName() + " was not in interaction for play command, sending discord message...");
			sendEmptyParameterMessage(e.getCausalParameterName(), embedMessage);
			return Optional.empty();
		} catch (BadAudioConnectionException e) {
			Logger.warn(e.getMessage());
			sendNotInServerVoiceChannelMessage(embedMessage);
			return Optional.empty();
		} catch (EmptyServerException e) {
			Logger.warn("Server was not present in an interaction!");
			sendEmptyServerMessage(embedMessage);
			return Optional.empty();
		}
		return Optional.of(ensuredInteraction);
	}

	private void sendQueueResultEmbed(EmbedMessage embedMessage, QueueResult queueResult) {
		// check if the track(s) went into the AudioQueue by flipping willPlayNow()
		embedMessage.setIsQueue(!queueResult.willPlayNow());
		
		// if the tracks were successfully queued & the size of the queue is greater than 1, just display the # of tracks added.
		if(queueResult.isSuccess() && queueResult.getQueuedTracks().size() > 1) {
			embedMessage.setTitle("Queued!");
			embedMessage.setContent("Added " + queueResult.getQueuedTracks().size() + " tracks to the queue."); 
		}
		// otherwise, if the track added successfully & it was only 1 track, use the setupAudioTrack method to display a single track!
		else if(queueResult.isSuccess() && queueResult.getQueuedTracks().size() == 1) {
			embedMessage.setupAudioTrack(queueResult.getQueuedTracks().get(0));
		}
		else if(!queueResult.isSuccess()) {
			embedMessage.setColor(Color.RED);
			embedMessage.setTitle("Oops!");
			embedMessage.setContent("There was an issue finding your query!");
		}
		else {
			embedMessage.setColor(Color.RED);
			embedMessage.setTitle("Oops!");
			embedMessage.setContent("There was an uncaught edge case. Please report this to <@806350925723205642>!");
		}

		embedMessage.send();
	}

	private void sendNotInServerVoiceChannelMessage(EmbedMessage embedMessage) {
		embedMessage.setTitle("Denied!");
		embedMessage.setContent("You are either not in a voice channel, or not in the same voice channel the bot is active in.");
		embedMessage.setColor(Color.RED);
		embedMessage.send();
	}

	private void sendEmptyParameterMessage(String parameter, EmbedMessage embedMessage) {
		embedMessage.setTitle("Missing Parameter(s)!");
		embedMessage.setContent("Parameter \"" + parameter + " was missing! Please include all required parameters in your command and try again.");
		embedMessage.setColor(Color.RED);
		embedMessage.send();
	}

	private void sendEmptyServerMessage(EmbedMessage embedMessage) {
		embedMessage.setTitle("Uh Oh!");
		embedMessage.setContent("The server was not present in the interaction. This shouldn't happen, but in the case you see this contact <@806350925723205642>.");
		embedMessage.send();
	}

	private void listenForCommands() {
		discordApi.addSlashCommandCreateListener(slashCommandEvent -> {
			// we always want to respond later, these commands (could) be quite heavy
			CompletableFuture<InteractionOriginalResponseUpdater> respondLater = slashCommandEvent.getSlashCommandInteraction().respondLater();
			// get slash command name
			String slashCommandName = slashCommandEvent.getSlashCommandInteraction().getCommandName();

			switch(slashCommandName) {
				case PLAY_COMMAND_NAME:
					handlePlayCommand(slashCommandEvent, respondLater);
					break;
				case INVITE_COMMAND_NAME:
					handleInviteCommand(slashCommandEvent, respondLater);
					break;
			}
		});
	}
}
