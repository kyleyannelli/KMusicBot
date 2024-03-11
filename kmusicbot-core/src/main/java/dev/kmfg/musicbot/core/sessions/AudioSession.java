package dev.kmfg.musicbot.core.sessions;

import dev.kmfg.musicbot.core.lavaplayer.AudioTrackWithUser;
import dev.kmfg.musicbot.core.lavaplayer.LavaSource;
import dev.kmfg.musicbot.core.lavaplayer.PositionalAudioTrack;
import dev.kmfg.musicbot.core.songrecommender.RecommenderProcessor;
import dev.kmfg.musicbot.core.songrecommender.RecommenderSession;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.hibernate.SessionFactory;
import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.user.User;
import org.tinylog.Logger;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import dev.kmfg.musicbot.database.models.DiscordUser;
import dev.kmfg.musicbot.core.util.sessions.LimitedQueue;
import dev.kmfg.musicbot.core.util.sessions.QueueResult;

public class AudioSession extends RecommenderSession {
	private static final int MAX_SEARCH_QUEUE_SIZE = 5; // a maximum of 5 items in mostRecentSearches
	private static final int DISCONNECT_DELAY_MS = 300_000; // 300_000 ms aka 5 minutes

	private final LimitedQueue<String> mostRecentSearches;
	private final ScheduledExecutorService disconnectScheduledService;
	private final SessionCloseHandler sessionCloseHandler;
	private final DiscordApi discordApi;
	private final SessionFactory sessionFactory;

	private LavaSource lavaSource;
	private AudioConnection audioConnection;

	private boolean isRecommendingSongs;

	public AudioSession(SessionFactory sessionFactory, DiscordApi discordApi, RecommenderProcessor recommenderProcessor, LavaSource lavaSource, long associatedServerId, SessionCloseHandler sessionCloseHandler) {
		super(recommenderProcessor, associatedServerId);

		this.mostRecentSearches = new LimitedQueue<>(MAX_SEARCH_QUEUE_SIZE);

		this.isRecommendingSongs = true;
		this.sessionFactory = sessionFactory;
		this.lavaSource = lavaSource;

		this.disconnectScheduledService = Executors.newSingleThreadScheduledExecutor();
		this.disconnectScheduledService.scheduleAtFixedRate(this::handleDisconnectService, DISCONNECT_DELAY_MS, DISCONNECT_DELAY_MS, TimeUnit.MILLISECONDS);

		this.sessionCloseHandler = sessionCloseHandler;

		this.discordApi = discordApi;
	}

	public AudioSession(SessionFactory sessionFactory, DiscordApi discordApi, RecommenderProcessor recommenderProcessor, long associatedServerId, SessionCloseHandler sessionCloseHandler) {
		super(recommenderProcessor, associatedServerId);

		this.mostRecentSearches = new LimitedQueue<>(MAX_SEARCH_QUEUE_SIZE);

		this.isRecommendingSongs = true;
		this.sessionFactory = sessionFactory;

		this.disconnectScheduledService = Executors.newSingleThreadScheduledExecutor();
		this.disconnectScheduledService.scheduleAtFixedRate(this::handleDisconnectService, DISCONNECT_DELAY_MS, DISCONNECT_DELAY_MS, TimeUnit.MILLISECONDS);

		this.sessionCloseHandler = sessionCloseHandler;

		this.discordApi = discordApi;
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
		return new ArrayList<>(mostRecentSearches);
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
        if(!this.isRecommending()) return;
		SecureRandom secureRandom = new SecureRandom();

		int lowerRandomBoundMs = 1000 * 10; // ms, 10 second
		int upperRandomBoundMs = 1000 * 60; // ms, 60 seconds aka 1 minute

		for(String title : recommendedTitles) {
			int randomVariation = lowerRandomBoundMs + secureRandom.nextInt(upperRandomBoundMs - lowerRandomBoundMs);

			Thread.sleep(YOUTUBE_SEARCH_SLEEP_DURATION_MS + randomVariation);
            if(!this.isRecommending()) return;

			User user = this.audioConnection.getServer().getApi().getYourself();
			DiscordUser discordUser = new DiscordUser(user.getId(), user.getDiscriminatedName());

			// we want the recommended songs to be on the non priority queue
			this.lavaSource.queueTrack(title, true, false, discordUser);
		}
	}

	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}

	@Override
	public ArrayList<AudioTrack> getAudioQueue() {
		return new ArrayList<>(this.lavaSource.getAudioQueue().stream()
				.map(AudioTrackWithUser::getAudioTrack)
				.collect(Collectors.toList()));
	}

	/**
	 * Toggles the isRecommendingSongs variable and returns new value
	 * @return boolean, will be of new value
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

	public QueueResult queueSearchQuery(DiscordUser discordUser, String searchQuery) {
		mostRecentSearches.add(searchQuery);
		return lavaSource.queueTrackAsPriority(searchQuery, discordUser);
	}

	public QueueResult queueSearchQueryNext(String searchQuery, DiscordUser discordUser) {
		mostRecentSearches.add(searchQuery);
		return lavaSource.queueTrackAsPriorityNext(searchQuery, discordUser);
	}

	/**
	 * @return QueueResult array, returns the skipped track in [0], returns the new track in [1]
	 */
	public QueueResult[] skipCurrentPlaying() {
		AudioTrack skippedTrack = this.lavaSource.skipCurrentTrack();
		AudioTrack newTrack = this.lavaSource.getCurrentPlayingAudioTrack();

		QueueResult[] queueResults = new QueueResult[2];
		// only success if something was playing, is not playing now because it was skipped
		queueResults[0] = new QueueResult(skippedTrack != null, false, skippedTrack);
		// if exists, it will play now
		queueResults[1] = new QueueResult(newTrack != null, newTrack != null, newTrack);

		return queueResults;
	}

	public LavaSource getLavaSource() {
		return lavaSource;
	}

	public ArrayList<PositionalAudioTrack> getPositionalAudioQueue() {
		return this.lavaSource.getPositionalAudioQueue();
	}

	public void setupAudioConnection(ServerVoiceChannel serverVoiceChannel) {
		AudioConnection audioConnection;

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
	 * @param user, the user we are checking
	 * @return boolean, true if user is in same voice channel (or voice channel at all), false if not
	 */
	public boolean isUserInSameServerVoiceChannel(User user) {
		ServerVoiceChannel botConnectedServerVoiceChannel = this.audioConnection.getChannel();

		Optional<ServerVoiceChannel> userConnectedServerVoiceChannel = user.getConnectedVoiceChannel(botConnectedServerVoiceChannel.getServer());

		return userConnectedServerVoiceChannel.isPresent() && userConnectedServerVoiceChannel.get().getId() == botConnectedServerVoiceChannel.getId();
	}

	@Override
	public String toString() {
		String first = "\tAudioSession | ID: " + this.getSessionId() + " | SERVER ID: " + this.getAssociatedServerId() + " |\n";
		String second = "\tCurrent Playing Track: " + this.lavaSource.getCurrentPlayingTrack() + "\n";
		String third = "\t" + (this.lavaSource.getAudioQueue() == null ? 0 : this.lavaSource.getAudioQueue().size()) + " tracks in the queue.";

		return first + second + third;
	}

	@Override
	public void shutdown() {
        // make sure we properly get tracking completed
        this.lavaSource.stop();
        this.skipCurrentPlaying();
        try {
            Thread.sleep(1500);
        }
        catch(InterruptedException iE) {
            Logger.error(iE, "AudioSession shutdown was interrupted!");
            Thread.currentThread().interrupt();
        }
		// shutdown the super classes stuff! This is important!
		super.shutdown();
		// now shutdown our new services
		this.disconnectScheduledService.shutdownNow();
		// shutdown the trackscheduler
		this.lavaSource.shutdown();

		this.sessionCloseHandler.handle(this.getAssociatedServerId());

		Logger.info("SHUTDOWN \n" + this);
	}

    public Optional<AudioTrackWithUser> remove(int position) {
        return this.lavaSource.remove(position);
    }

	public void properlyDisconnectFromVoiceChannel() {
		this.audioConnection.close().thenRun(this::shutdown);
	}

	public DiscordApi getDiscordApi() {
		return this.discordApi;
	}

    public void setIsRecommending(boolean is) {
        this.isRecommendingSongs = is;
    }

    public boolean isRecommending() {
        return this.isRecommendingSongs;
    }

    public void stopAllTracks() {
        this.isRecommendingSongs = false;
        this.getLavaSource().stop();
        this.skipCurrentPlaying();
        this.clearSearchHistory();
        
        try {
            Thread.sleep(500);
        }
        catch(InterruptedException iE) {
            Logger.error(iE, "Occurred in AudioSession when stopping all tracks.");
            Thread.currentThread().interrupt();
        }
    }

	private void handleDisconnectService() {
		if(canDisconnectFromVoiceChannel()) {
			Logger.info("DISCONNECTING \n" + this);
			this.properlyDisconnectFromVoiceChannel();
		}
		else {
			Logger.info("Did NOT Disconnect: \n" + this);
		}
	}

	private boolean canDisconnectFromVoiceChannel() {
		return !this.lavaSource.hasCurrentPlayingTrack() && this.lavaSource.isAudioQueueEmpty();
	}
}
