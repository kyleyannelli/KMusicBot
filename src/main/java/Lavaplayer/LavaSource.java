package DiscordBot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.hc.core5.http.ParseException;
import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.audio.AudioSourceBase;
import org.tinylog.Logger;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;

import Helpers.QueueResult;
import Lavaplayer.ProperTrackScheduler;
import SpotifyApi.HandleSpotifyLink;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;

public class LavaSource extends AudioSourceBase {
    private final AudioLoadResultHandler singleResultHandler = new AudioLoadResultHandler() {
        @Override
        public void loadFailed(FriendlyException arg0) {
            Logger.error(arg0, "Track failed to load!"); 
            isSuccess = false;
        }

        @Override
        public void noMatches() {
            Logger.warn("No matches found for query!"); 
            isSuccess = false;
        }

        @Override
        public void playlistLoaded(AudioPlaylist arg0) {
            ArrayList<String> loadedTracks = new ArrayList<>();
            // if theres a list of tracks AND first result, play the track
            if(arg0.getTracks() != null && arg0.getTracks().get(0) != null) {
                trackScheduler.loadSingleTrack(arg0.getTracks().get(0));
                loadedTracks.add(arg0.getTracks().get(0).getInfo().title);
                isSuccess = true;
            }
            lastLoadedTracks = Optional.of(loadedTracks);
        }

        @Override
        public void trackLoaded(AudioTrack arg0) {
            trackScheduler.loadSingleTrack(arg0);
            isSuccess = true;
            ArrayList<String> loadedTracks = new ArrayList<>();
            loadedTracks.add(arg0.getInfo().title);
            lastLoadedTracks = Optional.of(loadedTracks);
        }
    };

    private final AudioLoadResultHandler youtubeLinkResultHandler = new AudioLoadResultHandler() {
        @Override
        public void loadFailed(FriendlyException arg0) {
            Logger.error(arg0, "Track failed to load!"); 
            isSuccess = false;
        }

        @Override
        public void noMatches() {
            Logger.warn("No matches found for query!"); 
            isSuccess = false;
        }

        @Override
        public void playlistLoaded(AudioPlaylist arg0) {
            isSuccess = trackScheduler.loadPlaylist(arg0);
        }

        @Override
        public void trackLoaded(AudioTrack arg0) {
            trackScheduler.loadSingleTrack(arg0);
            isSuccess = true;
        }
    };

    private final AudioPlayerManager audioPlayerManager;
    private final AudioPlayer audioPlayer;
    private final ProperTrackScheduler trackScheduler;
    private AudioFrame lastFrame;

    private final SpotifyApi spotifyApi;
    private final DiscordApi discordApi;
    private boolean isSuccess;
    private Optional<List<String>> lastLoadedTracks;


    public LavaSource(DiscordApi discordApi, SpotifyApi spotifyApi, AudioPlayerManager audioPlayerManager) {
        super(discordApi);

        this.spotifyApi = spotifyApi;
        this.discordApi = discordApi;

        this.audioPlayerManager = audioPlayerManager;
        this.audioPlayer = audioPlayerManager.createPlayer();
        this.trackScheduler = new ProperTrackScheduler(audioPlayer);
        this.lastLoadedTracks = Optional.empty();
    }

    public LavaSource(DiscordApi discordApi, SpotifyApi spotifyApi, AudioPlayerManager audioPlayerManager, AudioPlayer audioPlayer) {
        super(discordApi);

        this.spotifyApi = spotifyApi;
        this.discordApi = discordApi;

        this.audioPlayerManager = audioPlayerManager;
        this.audioPlayer = audioPlayer; 
        this.trackScheduler = new ProperTrackScheduler(audioPlayer);
        this.lastLoadedTracks = Optional.empty();
    }

    @Override
    public byte[] getNextFrame() {
        if (lastFrame == null) {
            return null;
        }
        return applyTransformers(lastFrame.getData());
    }

    @Override
    public boolean hasFinished() {
        return false;
    }

    @Override
    public boolean hasNextFrame() {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Override
    public AudioSource copy() {
        return new LavaSource(discordApi, spotifyApi,audioPlayerManager, audioPlayer);
    }

    public synchronized QueueResult queueTrack(String searchQuery) {
        lastLoadedTracks = Optional.empty();
        isSuccess = false;

        Future<Void> playerManagerFuture = null;
        // if given a youtube link
        if(searchQuery.startsWith("https://youtube.com/")) {
            // is success is set by result handler
            playerManagerFuture = audioPlayerManager.loadItem(searchQuery, youtubeLinkResultHandler);
        }
        // if given a spotify link
        else if(searchQuery.startsWith("https://open.spotify.com/")) {
            lastLoadedTracks = getTracksFromSpotifyLink(searchQuery);    
            isSuccess = lastLoadedTracks.isPresent() && lastLoadedTracks.get().size() > 0;
        }
        // otherwise it will be treated as a text search
        else {
            // update search query to use ytsearch
            searchQuery = "ytsearch:" + searchQuery;
            // find by singleResultHandler
            // is success is set by result handler
            playerManagerFuture = audioPlayerManager.loadItem(searchQuery, singleResultHandler);
        }

        QueueResult futureQueueResult = handlePlayerManagerFuture(playerManagerFuture);

        return futureQueueResult == null ? new QueueResult(isSuccess, lastLoadedTracks.orElse(Collections.emptyList())) : futureQueueResult;
    }

    private QueueResult handlePlayerManagerFuture(Future<Void> playerManagerFuture) {
        if(playerManagerFuture != null) {
            try {
                // wait for the playerManager to load the track(s)
                playerManagerFuture.get();
            }
            catch(InterruptedException interruptedException) {
                Logger.error(interruptedException, "Interrupted while waiting for audioPlayerManager!");
                Thread.currentThread().interrupt();
                return new QueueResult(false, Collections.emptyList());
            }
            catch(ExecutionException executionException) {
                Logger.error(executionException, "Execution exception while waiting for audioPlayerManager!");
                return new QueueResult(false, Collections.emptyList());
            }
        }
        return null;
    }

    private Optional<List<String>> getTracksFromSpotifyLink(String spotifyLink) {
        Optional<List<String>> trackNamesFromSpotify = Optional.empty(); 

        try {
            trackNamesFromSpotify = Optional.ofNullable(HandleSpotifyLink.getCollectionFromSpotifyLink(spotifyApi, spotifyLink));
        }
        catch(IOException ioException) {
            Logger.error(ioException, "IOException occured while loading a spotify link " + spotifyLink);
        }
        catch(ParseException parseException) {
            Logger.error(parseException, "ParseException occured while loading a spotify link " + spotifyLink);
        }
        catch(SpotifyWebApiException spotifyWebApiException) {
            Logger.error(spotifyWebApiException, "SpotifyWebApiException occured while loading a spotify link " + spotifyLink);
        }

        return trackNamesFromSpotify;
    }
}

