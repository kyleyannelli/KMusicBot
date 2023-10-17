package dev.kmfg.sessions;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.javacord.api.DiscordApi;
import org.tinylog.Logger;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

import dev.kmfg.lavaplayer.LavaSource;
import dev.kmfg.songrecommender.RecommenderProcessor;
import se.michaelthelin.spotify.SpotifyApi;

public class SessionManager {
	private final ConcurrentHashMap<Long, AudioSession> audioSessions;
	private final RecommenderProcessor recommenderProcessor;
	private final DiscordApi discordApi;
	private final SpotifyApi spotifyApi;
	private final AudioPlayerManager audioPlayerManager;

	public SessionManager(DiscordApi discordApi, SpotifyApi spotifyApi, AudioPlayerManager audioPlayerManager, RecommenderProcessor recommenderProcessor) {
		this.audioSessions = new ConcurrentHashMap<>();
		this.recommenderProcessor = recommenderProcessor;
		this.audioPlayerManager = audioPlayerManager;
		this.discordApi = discordApi;
		this.spotifyApi = spotifyApi;
	}

	public AudioSession createAudioSession(long serverId) {
		AudioSession audioSession = new AudioSession(this.recommenderProcessor, serverId, this::handleAudioSessionShutdown);
		LavaSource lavaSource = new LavaSource(discordApi, spotifyApi, audioPlayerManager, audioSession.getSessionId());
		audioSession.setLavaSource(lavaSource);
		this.audioSessions.put(serverId, audioSession);
		return audioSession;
	}

	public AudioSession getAudioSession(long serverId) {
		return this.audioSessions.get(serverId);
	}

	public DiscordApi getDiscordApi() {
		return this.discordApi;
	}

	public int getTotalSessionCount() {
		return this.audioSessions.size();
	}

	public Optional<AudioSession> getOnlyAudioSession() {
		// if there isn't just one element, we don't want to return anything
		if(this.audioSessions.size() != 1) return Optional.empty();

		return Optional.of(
				this.audioSessions.elements().nextElement()
				);
	}

	private void handleAudioSessionShutdown(long associatedServerId) {
		// remove the audiosession upon shutdown
		long sessionId = this.audioSessions.get(associatedServerId).getSessionId();
		this.audioSessions.remove(associatedServerId);
		Logger.info("Session " + sessionId + " removed from server " + associatedServerId);
	}
}
