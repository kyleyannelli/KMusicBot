package Lavaplayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import AudioResultHandlers.SingleAudioResultHandler;
import AudioResultHandlers.YoutubeAudioResultHandler;
import Exceptions.AlreadyAccessedException;
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
import SpotifyApi.HandleSpotifyLink;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;

public class LavaSource extends AudioSourceBase {
    private final long ASSOCIATED_SESSION_ID;

    private final SingleAudioResultHandler singleResultHandler;

    private final YoutubeAudioResultHandler youtubeLinkResultHandler;

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
        return new LavaSource(discordApi, spotifyApi,audioPlayerManager, audioPlayer, ASSOCIATED_SESSION_ID);
    }

    public synchronized QueueResult queueTrack(String searchQuery) {
        Optional<ArrayList<String>> lastLoadedTracks = Optional.empty();
        boolean isSuccess = false;

        Future<Void> playerManagerFuture = null;
        boolean isYoutubeLink =  false;
        // if given a youtube link
        if(searchQuery.startsWith("https://youtube.com/")) {
            // is success is set by result handler
            playerManagerFuture = audioPlayerManager.loadItem(searchQuery, youtubeLinkResultHandler);
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

        QueueResult futureQueueResult = handlePlayerManagerFuture(playerManagerFuture, isYoutubeLink);

        return futureQueueResult;
    }

    private QueueResult handlePlayerManagerFuture(Future<Void> playerManagerFuture, boolean isYouTubeLink) {
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

        return generateQueueResultFromResultHandler(isYouTubeLink);
    }

    private QueueResult generateQueueResultFromResultHandler(boolean isYouTubeLink) {
        // if it was youtube link, pull from YoutubeResultHandler
        try {
            if(isYouTubeLink) {
                return new QueueResult(youtubeLinkResultHandler.getIsSuccess(), youtubeLinkResultHandler.getLastLoadedTracks());
            }
            else {
                return new QueueResult(singleResultHandler.getIsSuccess(), singleResultHandler.getLastLoadedTracks());
            }
        }
        catch(AlreadyAccessedException alreadyAccessedException) {
            Logger.warn(alreadyAccessedException, "Value for Result Handler was already accessed!");
            return null;
        }
    }

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

