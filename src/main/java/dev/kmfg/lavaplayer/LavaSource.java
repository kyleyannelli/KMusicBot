package dev.kmfg.lavaplayer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import dev.kmfg.lavaplayer.audioresulthandlers.SearchResultAudioHandler;
import dev.kmfg.lavaplayer.audioresulthandlers.SingleAudioResultHandler;
import dev.kmfg.lavaplayer.audioresulthandlers.YoutubeAudioResultHandler;
import dev.kmfg.exceptions.AlreadyAccessedException;
import org.apache.hc.core5.http.ParseException;
import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.audio.AudioSourceBase;
import org.tinylog.Logger;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;

import dev.kmfg.helpers.sessions.QueueResult;
import dev.kmfg.spotifyapi.HandleSpotifyLink;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;

/**
 * Handles search queries to the AudioSource with SingleResultHandler and YoutubeResultHandler.
 * Use this class to make queries and get audio streamed to a ServerVoiceChannel.
 */
public class LavaSource extends AudioSourceBase {
    private final long ASSOCIATED_SESSION_ID;

    private final SingleAudioResultHandler singleResultHandler;

    private final YoutubeAudioResultHandler youtubeLinkResultHandler;
    private final SearchResultAudioHandler searchResultAudioHandler;

    private final AudioPlayerManager audioPlayerManager;
    private final AudioPlayer audioPlayer;
    private final ProperTrackScheduler trackScheduler;
    private AudioFrame lastFrame;

    private final SpotifyApi spotifyApi;
    private final DiscordApi discordApi;


    public LavaSource(DiscordApi discordApi, SpotifyApi spotifyApi, AudioPlayerManager audioPlayerManager, long associatedSessionId) {
        super(discordApi);

        this.spotifyApi = spotifyApi;
        this.discordApi = discordApi;

        this.audioPlayerManager = audioPlayerManager;
        this.audioPlayer = audioPlayerManager.createPlayer();
        this.trackScheduler = new ProperTrackScheduler(audioPlayer, associatedSessionId);

        this.ASSOCIATED_SESSION_ID = associatedSessionId;

        this.singleResultHandler = new SingleAudioResultHandler(trackScheduler);
        this.youtubeLinkResultHandler = new YoutubeAudioResultHandler(trackScheduler);
        this.searchResultAudioHandler = new SearchResultAudioHandler(trackScheduler);
    }

    public LavaSource(DiscordApi discordApi, SpotifyApi spotifyApi, AudioPlayerManager audioPlayerManager, AudioPlayer audioPlayer, long associatedSessionId) {
        super(discordApi);

        this.spotifyApi = spotifyApi;
        this.discordApi = discordApi;

        this.audioPlayerManager = audioPlayerManager;
        this.audioPlayer = audioPlayer; 
        this.trackScheduler = new ProperTrackScheduler(audioPlayer, associatedSessionId);

        this.ASSOCIATED_SESSION_ID = associatedSessionId;

        this.singleResultHandler = new SingleAudioResultHandler(trackScheduler);
        this.youtubeLinkResultHandler = new YoutubeAudioResultHandler(trackScheduler);
        this.searchResultAudioHandler = new SearchResultAudioHandler(trackScheduler);
    }

    /**
     * Shuffles the user priority queue
     * @return int Of how many items were shuffled
     */
    public int shufflePriorityQueue() {
        // track scheduler priority queue returns the number of items shuffled
        return this.trackScheduler.shufflePriorityQueue();
    }

    /**
     * Checks if there is a next frame. If so it applies transformers, else returns null.
     * @return byte[], applied transformers if the next frame exists.
     */
    @Override
    public byte[] getNextFrame() {
        if (lastFrame == null) {
            return null;
        }
        return applyTransformers(lastFrame.getData());
    }

    /**
     * This is a constant of false.
     * @return boolean, false
     */
    @Override
    public boolean hasFinished() {
        return false;
    }

    /**
     * Checks the audio player to see if it has a next frame.
     * @return boolean
     */
    @Override
    public boolean hasNextFrame() {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    /**
     * Creates a new Lavasource object with the exact data.
     * @return AudioSource, LavaSource in this case (subclass).
     */
    @Override
    public AudioSource copy() {
        return new LavaSource(discordApi, spotifyApi,audioPlayerManager, audioPlayer, ASSOCIATED_SESSION_ID);
    }

    /**
     * Uses SearchResultAudioHandler to list results for a search query
     * @return QueueResult containing what was found (not loaded)
     */
    public synchronized QueueResult getListQueryResults(String searchQuery) {
        Future<Void> searchResultAudioHandlerFuture = audioPlayerManager.loadItem("ytsearch:" + searchQuery, this.searchResultAudioHandler);
        return handleSearchResultFuture(searchResultAudioHandlerFuture);
    }

    private QueueResult handleSearchResultFuture(Future<Void> searchResultAudioHandlerFuture) {
        // attempt to wait for future
        try {
            searchResultAudioHandlerFuture.get();
        }
        catch(InterruptedException interruptedException) {
            Logger.error(interruptedException, "Interrupted while waiting for search result handler!");
            Thread.currentThread().interrupt();
            return new QueueResult(false, false, Collections.emptyList());
        }
        catch(ExecutionException executionException) {
            Logger.error(executionException, "Execution exception while waiting for search result handler!");
            return new QueueResult(false, false, Collections.emptyList());
        }
        // items should be loaded by now.
        return generateQueueResultFromSearch();
    }

    private QueueResult generateQueueResultFromSearch() {
        try {
            boolean wasSuccess = this.searchResultAudioHandler.getIsSuccess();
            boolean willPlayNow = false;
            ArrayList<AudioTrack> foundTracks = this.searchResultAudioHandler.getLastLoadedTracks();
            return new QueueResult(wasSuccess, willPlayNow, foundTracks);
        }
        catch(AlreadyAccessedException alreadyAccessedException) {
            // already accessed exception will contain what value if thrown properly
            Logger.error(alreadyAccessedException, "Value for searchResultAudioHandler was already accessed!\nSearch attempt failed!");
            return new QueueResult(false, false, Collections.emptyList());
        }
    }

    /**
     * Checks if a string is one of the many valid youtube links
     */
    public boolean isYoutubeLink(String potentialLink) {
        return potentialLink.startsWith("https://youtube.com/") ||
            potentialLink.startsWith("https://www.youtube.com/") ||
            potentialLink.startsWith("https://youtu.be/") ||
            potentialLink.startsWith("https://www.youtu.be");
    }

    /**
     * Queues a track based on the searchQuery parameter. This is a thread safe function.
     * @param searchQuery the query you would like to search for
     * @param deprioritizeQueue tell the ResultHandler whether to put the track at the head queue (false, priority queue) or tail queue (true, deprioritized queue)
     * @param playNext tells the ResultHandler whether to put the track at the head (true) or tail (false) of the priority queue.
     * @return QueueResult, detailing what tracks were added.
     */
    public synchronized QueueResult queueTrack(String searchQuery, boolean deprioritizeQueue, boolean playNext) {
        boolean isYoutubeLink =  false;
        boolean willPlayNow = this.audioPlayer.getPlayingTrack() == null;

        Future<Void> playerManagerFuture = null;

        // update the queue priority
        if(deprioritizeQueue) {
            this.youtubeLinkResultHandler.deprioritizeQueue();
            this.singleResultHandler.deprioritizeQueue();
        } else {
            this.youtubeLinkResultHandler.prioritizeQueue();
            this.singleResultHandler.prioritizeQueue();
        }

        // update playNext
        this.youtubeLinkResultHandler.setPlayNext(playNext);
        this.singleResultHandler.setPlayNext(playNext);

        // if given a youtube link
        if(this.isYoutubeLink(searchQuery)) {
            int indexOfFirstParameter = searchQuery.indexOf('&');
            // chop off any parameters
            String parameterlessSearchQuery = indexOfFirstParameter != -1 ?
                searchQuery.substring(0, indexOfFirstParameter) : searchQuery;
            // is success is set by result handler
            playerManagerFuture = audioPlayerManager.loadItem(parameterlessSearchQuery, youtubeLinkResultHandler);
            isYoutubeLink = true;
        }
        // if given a spotify link
        else if(searchQuery.startsWith("https://open.spotify.com/")) {
            //            lastLoadedTracks = getTracksFromSpotifyLink(searchQuery);
            //            isSuccess = lastLoadedTracks.isPresent() && lastLoadedTracks.get().size() > 0;

            // @TODO Implement spotify searches
        }
        // otherwise it will be treated as a text search
        else {
            // update search query to use ytsearch
            searchQuery = "ytsearch:" + searchQuery;
            // find by singleResultHandler
            // is success is set by result handler
            playerManagerFuture = audioPlayerManager.loadItem(searchQuery, singleResultHandler);
        }

        return handlePlayerManagerFuture(playerManagerFuture, isYoutubeLink, willPlayNow);
    }

    /**
     * Queue a track into the priority (aka user) queue. This is a thread safe function.
     * @param searchQuery The track you would like to find
     * @return QueueResult, detailing what was added to the queue.
     */
    public synchronized QueueResult queueTrackAsPriority(String searchQuery) {
        // 2nd parameter states we want to prioritize this query
        // 3rd parameter states we want to send the track to the tail of the queue
        return queueTrack(searchQuery, false, false);
    }

    /**
     * Queue a track into the priority at the head. This is a thread safe function.
     * @param searchQuery the track you want to search for
     * @return QueueResult, detailing what was added to the queue.
     */
    public synchronized QueueResult queueTrackAsPriorityNext(String searchQuery) {
        // 2nd parameter states we want to prioritize this query
        // 3rd parameter states we want to send the track to the head of the queue
        return queueTrack(searchQuery, false, true);
    }

    /**
     * Gets the audio queue from the track scheduler, including the priority and depriority queue.
     * The priority queue is at the head, deprioritized queue is at the tail.
     * @return ArrayList of AudioTrack
     */
    public ArrayList<AudioTrack> getAudioQueue() {
        return this.trackScheduler.getFullAudioQueue();
    }

    /**
     * Note: This is slower than the regular getAudioQueue function as it has to loop through each queue. Only use this if you need PositionalAudioTrack objects.
     * Gets the audio queue as PositionalAudioTrack from the track scheduler.
     * This includes the priority and depriority queue.
     * The priority queue is at the head, deprioritized queue is at the tail.
     * @return ArrayList of PositonalAudioTrack
     */
    public ArrayList<PositionalAudioTrack> getPositionalAudioQueue() {
        return this.trackScheduler.getPositionalAudioQueue();
    }

    /**
     * Skips the current playing track by stopping the current playing track.
     * @return AudioTrack, the track which was stopped.
     */
    public AudioTrack skipCurrentTrack() {
        if(this.audioPlayer.getPlayingTrack() == null) {
            return null;
        }
        else {
            AudioTrack stoppedTrack = this.audioPlayer.getPlayingTrack();
            // stop the current playing track, this will effectively "skip" due to ProperTrackScheduler logic.
            this.audioPlayer.stopTrack();
            return stoppedTrack;
        }
    }

    /**
     * Gets the current playing track from audioplayer.
     * @return AudioTrack, the current playing track. It is possible to be null.
     */
    public AudioTrack getCurrentPlayingAudioTrack() {
        return this.audioPlayer.getPlayingTrack();
    }

    /**
     * Checks if the AudioPlayer has a track currently playing.
     * 
     * @return boolean | True if playingTrack != null. False if playingTrack == null.
     */ 
    public boolean hasCurrentPlayingTrack() {
        return this.audioPlayer.getPlayingTrack() != null;
    }

    /**
     * Checks if the audioQueue is empty. This uses the getAudioQueue function, so the queue which is polled is dependent on that function.
     * @return boolean, true if it is empty (0 or null), false if it has at least 1 AudioTrack object.
     */
    public boolean isAudioQueueEmpty() {
        return (this.getAudioQueue() != null && this.getAudioQueue().size() <= 0) || this.getAudioQueue() == null;
    }

    /**
     * Gets the current playing track as its title, author, and uri in a string
     * @return String, contains the title, author, and uri.
     */
    public String getCurrentPlayingTrack() {
        return this.hasCurrentPlayingTrack() ? this.audioPlayer.getPlayingTrack().getInfo().title + " | " + this.audioPlayer.getPlayingTrack().getInfo().author + " | " + this.audioPlayer.getPlayingTrack().getInfo().uri  : "!Nothing Playing!";
    }

    /**
     * Handles the future from the player manager. Generates a QueueResult whether it fails (this is detailed by the QueueResult)
     * @param playerManagerFuture The future from the audioPlayerManager
     * @param isYouTubeLink Whether it is a youtube link, this is relevant to the QueueResult
     * @param willPlayNow Whether it will play now or got put in the queue, this is relevant to the QueueResult
     * @return QueueResult, detailing what was loaded.
     */
    private QueueResult handlePlayerManagerFuture(Future<Void> playerManagerFuture, boolean isYouTubeLink, boolean willPlayNow) {
        if(playerManagerFuture != null) {
            try {
                // wait for the playerManager to load the track(s)
                playerManagerFuture.get();
            }
            catch(InterruptedException interruptedException) {
                Logger.error(interruptedException, "Interrupted while waiting for audioPlayerManager!");
                Thread.currentThread().interrupt();
                return new QueueResult(false, false, Collections.emptyList());
            }
            catch(ExecutionException executionException) {
                Logger.error(executionException, "Execution exception while waiting for audioPlayerManager!");
                return new QueueResult(false, false, Collections.emptyList());
            }
        }

        return generateQueueResultFromResultHandler(isYouTubeLink, willPlayNow);
    }

    /**
     * Generates a QueueResult from the loaded track. Will return null if the result handler has an already accessed value.
     * @param isYouTubeLink Whether its a youtube link. This determines what result handler gets pulled from.
     * @param willPlayNow Whether it will play now or got put in the queue. This is relevant to the QueueResult.
     * @return QueueResult, detailing what was loaded.
     */
    private QueueResult generateQueueResultFromResultHandler(boolean isYouTubeLink, boolean willPlayNow) {
        // if it was youtube link, pull from YoutubeResultHandler
        try {
            if(isYouTubeLink) {
                return new QueueResult(youtubeLinkResultHandler.getIsSuccess(), willPlayNow, youtubeLinkResultHandler.getLastLoadedTracks());
            }
            else {
                return new QueueResult(singleResultHandler.getIsSuccess(), willPlayNow, singleResultHandler.getLastLoadedTracks());
            }
        }
        catch(AlreadyAccessedException alreadyAccessedException) {
            Logger.warn(alreadyAccessedException, "Value for Result Handler was already accessed!");
            return null;
        }
        catch(NullPointerException nullPointerException) {
            Logger.error(nullPointerException, "Null Pointer for generate queue. Likely no results found.");
            return null;
        }
    }

    /**
     * @TODO Function hasn't been implemented yet. Do I still want to support Spotify links?
     * @param spotifyLink
     * @return
     */
    private Optional<List<String>> getTracksFromSpotifyLink(String spotifyLink) {
        Optional<List<String>> trackNamesFromSpotify = Optional.empty(); 

        try {
            trackNamesFromSpotify = Optional.ofNullable(HandleSpotifyLink.getCollectionFromSpotifyLink(spotifyApi, spotifyLink));
        }
        catch(IOException ioException) {
            Logger.error(ioException, "IOException occurred while loading a spotify link " + spotifyLink);
        }
        catch(ParseException parseException) {
            Logger.error(parseException, "ParseException occurred while loading a spotify link " + spotifyLink);
        }
        catch(SpotifyWebApiException spotifyWebApiException) {
            Logger.error(spotifyWebApiException, "SpotifyWebApiException occurred while loading a spotify link " + spotifyLink);
        }

        return trackNamesFromSpotify;
    }
}

