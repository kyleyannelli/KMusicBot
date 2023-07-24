package DiscordBot;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerVoiceChannel;
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

public class Commands {
	private static final String PLAY_COMMAND_NAME = "play";

	private final DiscordApi discordApi;
	private final SpotifyApi spotifyApi;
	private final RecommenderProcessor recommenderProcessor;
	private final AudioPlayerManager audioPlayerManager;

	private final HashMap<Long, AudioSession> audioSessions;

	public Commands(DiscordApi discordApi, SpotifyApi spotifyApi, RecommenderProcessor recommenderProcessor, AudioPlayerManager audioPlayerManager) {
		this.discordApi = discordApi;
		this.spotifyApi = spotifyApi;
		this.recommenderProcessor = recommenderProcessor;
		this.audioPlayerManager = audioPlayerManager;
		this.audioSessions = new HashMap<>();
	}

	public void createAndListenForGlobalCommands() {
		createPlayCommand();

		listenForCommands();
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
		// check if the server is empty, this will handle sending a message if needed.
		if(handleEmptyServerInInteraction(slashCommandEvent, respondLater)) return;

		// check if the parameter is empty
		Optional<String> searchQuery = slashCommandEvent.getSlashCommandInteraction().getArgumentStringValueByName("song");
		if(handleEmptyParameter(searchQuery, slashCommandEvent, respondLater)) return;

		// get the server and user as they will be used throughout
		Server server = slashCommandEvent.getInteraction().getServer().get();
		User requestingUser = slashCommandEvent.getInteraction().getUser();

		AudioSession relevantAudioSession = audioSessions
			.computeIfAbsent(server.getId(), this::createAudioSession);

		if(handleBadAudioConnection(relevantAudioSession, server, requestingUser, slashCommandEvent, respondLater)) return;

		EmbedMessage embedMessage = new EmbedMessage(slashCommandEvent.getSlashCommandInteraction(), respondLater);
		QueueResult queueResult = relevantAudioSession.queueSearchQuery(searchQuery.get());

		sendQueueResultEmbed(embedMessage, queueResult);
	}

	private boolean handleBadAudioConnection(AudioSession audioSession, Server server, User user, SlashCommandCreateEvent slashCommandEvent, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
		if(audioSession.hasAudioConnection() && !audioSession.isUserInSameServerVoiceChannel(user)) {
			sendNotInServerVoiceChannelMessage(slashCommandEvent, respondLater);
			return true;
		}
		else if(!audioSession.hasAudioConnection() && user.getConnectedVoiceChannel(server).isPresent()) {
			// the presence of ServerVoiceChannel was checked in the previous if statement
			ServerVoiceChannel serverVoiceChannel = user.getConnectedVoiceChannel(server).get();
			audioSession.setupAudioConnection(serverVoiceChannel);
			return false;
		}
		else {
			sendNotInServerVoiceChannelMessage(slashCommandEvent, respondLater);
			return true;
		}
	}

	private void sendQueueResultEmbed(EmbedMessage embedMessage, QueueResult queueResult) {
		if(queueResult.isSuccess()) {
			embedMessage.setTitle("Queued!");
			embedMessage.setContent(queueResult.getQueuedTracks().size() > 1 ? "Queued " + queueResult.getQueuedTracks().size() + " tracks." : "Queued " + queueResult.getQueuedTracks().get(0));
		}
		else {
			embedMessage.setTitle("Uh Oh!");
			embedMessage.setContent("There was an issue finding your query!");
		}

		embedMessage.send();
	}

	private void sendNotInServerVoiceChannelMessage(SlashCommandCreateEvent slashCommandCreateEvent, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
		EmbedMessage embedMessage = new EmbedMessage(slashCommandCreateEvent.getSlashCommandInteraction(), respondLater);
		embedMessage.setTitle("Denied!");
		embedMessage.setContent("You are either not in a voice channel, or not in the same voice channel the bot is active in.");
		embedMessage.setColor(Color.RED);
		embedMessage.send();
	}

	private AudioSession createAudioSession(long serverId) {
		LavaSource lavaSource = new LavaSource(discordApi, spotifyApi, audioPlayerManager);
		return new AudioSession(this.recommenderProcessor, lavaSource, serverId);
	}

	private boolean handleEmptyParameter(Optional<String> parameter, SlashCommandCreateEvent slashCommandEvent, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
		if(parameter.isEmpty()) {
			EmbedMessage embedMessage = new EmbedMessage(slashCommandEvent.getSlashCommandInteraction(), respondLater);
			embedMessage.setTitle("Missing parameters!");
			embedMessage.setContent("A parameter(s) was missing! Please include all required parameters in your command and try again.");
			embedMessage.send();
			return true;
		}
		return false;
	}

	private boolean handleEmptyServerInInteraction(SlashCommandCreateEvent slashCommandEvent, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
		if(slashCommandEvent.getInteraction().getServer().isEmpty()) {
			EmbedMessage embedMessage = new EmbedMessage(slashCommandEvent.getSlashCommandInteraction(), respondLater);
			embedMessage.setTitle("Uh Oh!");
			embedMessage.setContent("The server was not present in the interaction. This shouldn't happen, but in the case you see this contact <@806350925723205642>.");
			embedMessage.send();

			Logger.warn("Server was empty in slash command interaction (this shouldn't happen!), id: " + slashCommandEvent.getInteraction().getIdAsString());
			return true;
		}
		return false;
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
			}
		});
	}
}
