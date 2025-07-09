package dev.kmfg.musicbot.core.songrecommender;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Currently, unsure about keeping this class. It should really be abstract.
 * However, it is the backbone of an
 * {@link dev.kmfg.musicbot.core.sessions.AudioSession}. This class provides a
 * way to agnostically find recommendations based on a given List<String> of
 * tracks.
 */
public abstract class RecommenderSession {
    public static final int MINIMUM_AUTO_QUEUE_DURATION_SECONDS = 900; // 900 seconds == 15 minutes
    public static final int MINIMUM_QUEUE_SIZE = 2; // at the least 4 songs must be queued
    public static final int MAXIMUM_QUEUE_SIZE = 25; // at the most 25 songs are queued
    public static final int YOUTUBE_SEARCH_SLEEP_DURATION_MS = 1000;
    public static final int AUTO_QUEUE_RATE = 5; // unit in minutes
    public static final int INITIAL_AUTO_QUEUE_DELAY = 1; // unit in minutes

    // there should only be one recommender processor per application.
    // the processor handles all sessions via an executor service
    private final RecommenderProcessor recommenderProcessor;

    private final long id;
    private final long associatedServerId;

    private final ScheduledExecutorService scheduler;

    private ArrayList<AudioTrack> searchedSongs;

    public RecommenderSession(RecommenderProcessor recommenderProcessor, long associatedServerId) {
        this.recommenderProcessor = recommenderProcessor;
        this.searchedSongs = new ArrayList<>();

        long lowerBound = 12345678L;
        long upperBound = 99999999L;
        Random random = new Random();
        this.id = lowerBound + (long) (random.nextDouble() * upperBound);

        Logger.info("Created id {} for session in server {}.", this.id, associatedServerId);

        this.associatedServerId = associatedServerId;

        // every AUTO_QUEUE_RATE minutes autoqueue
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        // the below will remain disabled until Spotify Web API gets replaced
        this.scheduler.scheduleAtFixedRate(
                this::loadRecommendedTracks,
                INITIAL_AUTO_QUEUE_DELAY,
                AUTO_QUEUE_RATE,
                TimeUnit.MINUTES
        );
    }

    public void addSearchToSearchedSongs(AudioTrack audioTrack) {
        this.searchedSongs.add(audioTrack);
    }

    public ArrayList<AudioTrack> getSearchedSongs() {
        return searchedSongs;
    }

    public ArrayList<AudioTrack> getAudioQueue() {
        return searchedSongs;
    }

    public void setAudioQueue(ArrayList<AudioTrack> audioQueue) {
        this.searchedSongs = audioQueue;
    }

    public void cancelAllOperations() {
        this.recommenderProcessor.cancelTasksBySessionId(this.id);
    }

    public abstract void addRecommendationsToQueue(String[] recommendedTitles) throws InterruptedException;

    public long getSessionId() {
        return id;
    }

    public void shutdown() {
        this.scheduler.shutdownNow();
    }

    public long getAssociatedServerId() {
        return associatedServerId;
    }

    public void clearSearchHistory() {
        this.searchedSongs = new ArrayList<>();
    }

    public boolean canQueueSongs() {
        // Calculate the total duration of all songs in the queue
        AtomicLong totalDuration = new AtomicLong();
        // for each AudioTrack (song) in the queue add their duration
        searchedSongs.forEach(song -> totalDuration.addAndGet(song.getDuration()));

        // the total duration of tracks is over 15 MINIMUM_AUTO_QUEUE_DURATION_SECONDS
        // OR the queue is 4 through 15 songs long
        return (totalDuration.get() > MINIMUM_AUTO_QUEUE_DURATION_SECONDS && searchedSongs.size() <= MAXIMUM_QUEUE_SIZE)
                || (searchedSongs.size() >= MINIMUM_QUEUE_SIZE && searchedSongs.size() <= MAXIMUM_QUEUE_SIZE);
    }

    private void loadRecommendedTracks() {
        if (canQueueSongs()) {
            // Process the songs from searched songs list
            this.recommenderProcessor.addRecommendedSongs(this);
        }
    }
}
