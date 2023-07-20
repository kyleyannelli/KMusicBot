package DiscordBot;

import SongRecommender.RecommenderProcessor;
import SongRecommender.RecommenderSession;

import java.util.ArrayList;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;

import Helpers.LimitedQueue;
import Lavaplayer.TrackScheduler;

public class AudioSession extends RecommenderSession {
	private static final int MAX_SEARCH_QUEUE_SIZE = 5; // a maxmimum of 5 items in mostRecentSearches
    private static final int DISCONNECT_DELAY_SECONDS = 300000; // 300000 seconds aka 5 minutes

	private final LimitedQueue<String> mostRecentSearches;

	private AudioPlayer audioPlayer;
	private TrackScheduler trackScheduler; 

	public AudioSession(RecommenderProcessor recommenderProcessor, long associatedServerId) {
		super(recommenderProcessor, associatedServerId);

		mostRecentSearches = new LimitedQueue<>(MAX_SEARCH_QUEUE_SIZE);

		this.audioPlayer = null;
		this.trackScheduler = null;
	}

	public AudioSession(RecommenderProcessor recommenderProcessor, AudioPlayer audioPlayer, TrackScheduler trackScheduler, long associatedServerId) {
		super(recommenderProcessor, associatedServerId);

		mostRecentSearches = new LimitedQueue<>(MAX_SEARCH_QUEUE_SIZE);

		this.audioPlayer = audioPlayer;
		this.trackScheduler = trackScheduler;
	}

	public void setAudioPlayer(AudioPlayer audioPlayer) {
		this.audioPlayer = audioPlayer;
	}

	public void setTrackScheduler(TrackScheduler trackScheduler) {
		this.trackScheduler = trackScheduler;
	}

	/**
	 * Overrides the getSearchedSongs which is used to determine searched songs by the processor.
	 * 	However, in an AudioSession we want to specifically use mostRecentSearches which is a {@link LimitedQueue} 
	 *
	 * @return {@link ArrayList}
	 */
	@Override
	public ArrayList<String> getSearchedSongs() {
		// LimitedQueue extends LinkedBlockingDeque which is an AbstractCollection
		return new ArrayList<String>(mostRecentSearches);
	}
}
