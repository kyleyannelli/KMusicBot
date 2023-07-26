package DiscordBot;

import Lavaplayer.LavaSource;
import SongRecommender.RecommenderProcessor;
import SongRecommender.RecommenderSession;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.javacord.api.audio.AudioConnection;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.user.User;
import org.tinylog.Logger;

import Helpers.LimitedQueue;
import Helpers.QueueResult;

public class AudioSession extends RecommenderSession {
	private static final int MAX_SEARCH_QUEUE_SIZE = 5; // a maximum of 5 items in mostRecentSearches
	private static final int DISCONNECT_DELAY_SECONDS = 300000; // 300000 seconds aka 5 minutes

	private final LimitedQueue<String> mostRecentSearches;
	private LavaSource lavaSource;
	private AudioConnection audioConnection;

	private boolean isRecommendingSongs;

	public AudioSession(RecommenderProcessor recommenderProcessor, LavaSource lavaSource, long associatedServerId) {
		super(recommenderProcessor, associatedServerId);

		mostRecentSearches = new LimitedQueue<>(MAX_SEARCH_QUEUE_SIZE);

		this.isRecommendingSongs = true;
		this.lavaSource = lavaSource;
	}

	public AudioSession(RecommenderProcessor recommenderProcessor, long associatedServerId) {
		super(recommenderProcessor, associatedServerId);

		mostRecentSearches = new LimitedQueue<>(MAX_SEARCH_QUEUE_SIZE);

		this.isRecommendingSongs = true;
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

	/**
	 * Checks if the mostRecentSearches (LimitedQueue) is above MAX_SEARCH_QUEUE_SIZE - 1 AND if song recommendations are enabled. 
	 * 
	 * @return true if mostRecentSearches total MAX_SEARCH_QUEUE_SIZE - 1 or greater AND song recommendations are enabled, false otherwise
	 */
	@Override
	public boolean canQueueSongs() {
		return (mostRecentSearches.size() >= MAX_SEARCH_QUEUE_SIZE - 2) && isRecommendingSongs && this.getAudioQueue().size() < 15;
	}

	@Override
	public void addRecommendationsToQueue(String[] recommendedTitles) throws InterruptedException {
		Random random = new Random();

		int lowerRandomBoundMs = 100; // ms
		int upperRandomBoundMs = 1000; // ms

		for(String title : recommendedTitles) {
			int randomVariation = lowerRandomBoundMs + (int) (random.nextDouble() * upperRandomBoundMs);

			Thread.sleep(YOUTUBE_SEARCH_SLEEP_DURATION_MS + randomVariation);

			this.lavaSource.queueTrack(title);	
		}
	}

	/**
	 * Toggles the isRecommendingSongs variable and returns new value
	 */
	public boolean toggleIsRecommending() {
		return (isRecommendingSongs = !isRecommendingSongs);
	}

	public boolean getIsRecommending() {
		return isRecommendingSongs;
	}

	public void setLavaSource(LavaSource lavaSource) {
		this.lavaSource = lavaSource;
	}

	public QueueResult queueSearchQuery(String searchQuery) {
		mostRecentSearches.add(searchQuery);
		return lavaSource.queueTrack(searchQuery);
	}

	public LavaSource getLavaSource() {
		return lavaSource;
	}

	public void setupAudioConnection(ServerVoiceChannel serverVoiceChannel) {
		AudioConnection audioConnection = null;

		CompletableFuture<AudioConnection> futureConnect = serverVoiceChannel.connect();
		
		try {
			audioConnection = futureConnect.get();
		}
		catch(ExecutionException exeException) {
			Logger.error(exeException, "ExecutionException while attempting to wait for voice channel connect for server " + serverVoiceChannel.getServer().getId() + "|" + serverVoiceChannel.getServer().getName());
			this.audioConnection = null;
			return;
		}
		catch(InterruptedException interruptedException) {
			Logger.error(interruptedException, "InterruptedException while attempting to wait for voice channel connect for server " + serverVoiceChannel.getServer().getId() + "|" + serverVoiceChannel.getServer().getName());
			Thread.currentThread().interrupt();
			this.audioConnection = null;
			return;
		}

		this.audioConnection = audioConnection;
		this.audioConnection.setAudioSource(lavaSource);
	}

	public boolean hasAudioConnection() {
		return this.audioConnection != null;
	}

	/**
	 * Checks if user is in server voice channel and if it is the same server voice channel as the bot.
	 */
	public boolean isUserInSameServerVoiceChannel(User user) {
		ServerVoiceChannel botConnectedServerVoiceChannel = this.audioConnection.getChannel();

		Optional<ServerVoiceChannel> userConnectedServerVoiceChannel = user.getConnectedVoiceChannel(botConnectedServerVoiceChannel.getServer());
		
		return userConnectedServerVoiceChannel.isPresent() && userConnectedServerVoiceChannel.get().getId() == botConnectedServerVoiceChannel.getId();
	}
}
