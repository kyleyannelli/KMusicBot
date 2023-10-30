package dev.kmfg.lavaplayer.audioresulthandlers;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import dev.kmfg.database.models.DiscordUser;
import dev.kmfg.helpers.sessions.SingleUse;
import dev.kmfg.lavaplayer.AudioTrackWithUser;
import dev.kmfg.lavaplayer.ProperTrackScheduler;

import java.util.*;

/**
 * Gets search queries, but does not actually load any tracks into the queue.
 * Track loading should be handled by the AudioPlayer
 */
public class SearchResultAudioHandler extends KAudioResultHandler {
    public SearchResultAudioHandler(ProperTrackScheduler trackScheduler, DiscordUser discordUser) {
        super(trackScheduler, discordUser);
    }

    /**
     * Takes a single audio track, then puts it  into the lastLoadedTracks
     * AudioTrack must not be null otherwise isSuccess is set to false and lastLoadedTracks will be empty
     * @param audioTrack the audio track which was loaded
     */
    @Override
    public void trackLoaded(AudioTrack audioTrack) {
        // make sure track isnt null
        if(audioTrack == null) {
            this.setupFieldsFromFailure();
            return;
        }

        AudioTrackWithUser audioTrackWithUser = new AudioTrackWithUser(audioTrack, discordUser);
        // create SingleUse  for the audio track into arraylist with  List.of
        this.lastLoadedTracks = new SingleUse<>(new ArrayList<>(List.of(audioTrackWithUser)));
        // was a success
        this.isSuccess = new SingleUse<>(true);
    }

    /**
     * Takes an AudioPlaylist and puts the track into lastLoadedTracks
     * Accepts any size List of AudioTracks, but will only insert 1 through 5
     * @param audioPlaylist the AudioPlaylist which holds AudioTracks
     */
    @Override
    public void playlistLoaded(AudioPlaylist audioPlaylist) {
        // get the tracks which were loaded from the AudioPlaylist object
        List<AudioTrack> tracks = audioPlaylist.getTracks();

        // if nothing is in the List, do an early return
        if(tracks == null || tracks.isEmpty()) {
            // use the from failure method
            this.setupFieldsFromFailure();
            return;
        }

        ArrayList<AudioTrackWithUser> foundTracks = new ArrayList<>();
        // loop either the size of the tracks List, but 5 at most
        int totalLoopTimes = Math.min(tracks.size(), 5);
        for(int i = 0; i < totalLoopTimes; i++) {
            // add the track from AudioPlaylist into foundTracks
            foundTracks.add(new AudioTrackWithUser(tracks.get(i), discordUser));

        }

        this.lastLoadedTracks = new SingleUse<>(foundTracks);
        // we have added the tracks, so at this point it is a success.
        this.isSuccess = new SingleUse<>(true);
    }

    /**
     * Sets up fields for failure case.
     */
    @Override
    public void noMatches() {
        // use the from failure method
        this.setupFieldsFromFailure();
    }

    /**
     * Setups up fields for failure case
     * @param e FriendlyException which caused the load failure
     */
    @Override
    public void loadFailed(FriendlyException e) {
        // use the from failure method
        this.setupFieldsFromFailure();
    }

    /**
     * Sets up the protected fields lastLoadedTracks and isSuccess for any type of failed load.
     */
    private void setupFieldsFromFailure() {
        // initialize with empty arraylist
        this.lastLoadedTracks = new SingleUse<>(new ArrayList<>());
        // was not a success
        this.isSuccess = new SingleUse<>(false);
    }
}
