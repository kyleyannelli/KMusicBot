package SongRecommender;

import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import se.michaelthelin.spotify.SpotifyApi;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class RecommenderSession {
	private final int MINIMUM_AUTO_QUEUE_DURATION_SECONDS = 900; // 900 seconds == 15 minutes
	private final int MINIMUM_QUEUE_SIZE = 4; // at the least 4 songs must be queued
	private final int MAXIMUM_QUEUE_SIZE = 15; // at the most 15 songs are queued

	// there should only be one recommender processor per application.
	// 	the processor handles all sessions via an executor service
	private final RecommenderProcessor recommenderProcessor;

	// AudioConnection should be persistent the entire time the bot is connected to voice channel
	//  Connect is new session, disconnect should "kill" session
	private final AudioConnection audioConnection;

	private ArrayList<AudioTrack> audioQueue; // audio queue for the voice channel session

	public RecommenderSession(AudioConnection audioConnection, RecommenderProcessor recommenderProcessor) {
		this.recommenderProcessor = recommenderProcessor;
		this.audioConnection = audioConnection;
	}


	public ArrayList<String> getSearchedSongs() {
		return new ArrayList<String>();
	}

	public ArrayList<AudioTrack> getAudioQueue() {
		return new ArrayList<AudioTrack>();
	}

	private void loadRecommendedTracks(DiscordApi api, SpotifyApi spotifyApi, AudioPlayerManager playerManager, AudioConnection audioConnection, SlashCommandCreateEvent event) {
		if(canQueueSongs()) {
			// Process the songs from searched songs list
			ArrayList<String> trackNames = recommenderProcessor.determineSongsFromYoutube(this);
			String[] recommendationsFromSpotify = recommenderProcessor.getRecommendationsFromSpotify(trackNames);
		}
	}

	private boolean canQueueSongs() {
		// Calculate the total duration of all songs in the queue
		AtomicLong totalDuration = new AtomicLong();
		// for each AudioTrack (song) in the queue add their duration
		audioQueue.forEach(song -> totalDuration.addAndGet(song.getDuration()));

		// the total duration of tracks is over 15 MINIMUM_AUTO_QUEUE_DURATION_SECONDSu
		// 	OR the queue is 4 through 15 songs long
		return totalDuration.get() > MINIMUM_AUTO_QUEUE_DURATION_SECONDS 
			|| (audioQueue.size() >= MINIMUM_QUEUE_SIZE && audioQueue.size() <= MAXIMUM_QUEUE_SIZE);
	}
}
