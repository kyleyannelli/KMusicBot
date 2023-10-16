package dev.kmfg.lavaplayer.audioresulthandlers;

import dev.kmfg.helpers.sessions.SingleUse;
import dev.kmfg.lavaplayer.ProperTrackScheduler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.ArrayList;
import java.util.List;

import org.tinylog.Logger;

public class YoutubeAudioResultHandler extends KAudioResultHandler {
    public YoutubeAudioResultHandler(ProperTrackScheduler trackScheduler) {
        super(trackScheduler);
    }

    @Override
    public void loadFailed(FriendlyException arg0) {
        Logger.error(arg0, "Track failed to load!");
        this.isSuccess = new SingleUse<>(false);
    }

    @Override
    public void noMatches() {
        Logger.warn("No matches found for query!");
        this.isSuccess = new SingleUse<>(false);
    }

    @Override
    public void playlistLoaded(AudioPlaylist arg0) {
        boolean loadPlaylistResult = trackScheduler.loadPlaylist(arg0, this.deprioritizeQueue, this.playNext);

        // log result
        if(loadPlaylistResult) {
            Logger.info("Playlist successfully loaded.");
            //ArrayList<AudioTrack> loadedTracks = new ArrayList(arg0.getTracks());
            ArrayList<AudioTrack> loadedTracks = new ArrayList<>();
            arg0.getTracks().stream().forEach(track -> loadedTracks.add(track));
            this.lastLoadedTracks = new SingleUse<>(loadedTracks);
        }
        else Logger.warn("Playlist failed to load!");

        this.isSuccess = new SingleUse<>(loadPlaylistResult);
    }

    @Override
    public void trackLoaded(AudioTrack arg0) {
        if(this.playNext && this.trackScheduler.hasNowPlaying()) {
            trackScheduler.queueNext(arg0);
        }
        else {
            trackScheduler.loadSingleTrack(arg0, this.deprioritizeQueue);
        }
        this.lastLoadedTracks = new SingleUse<>(new ArrayList<>(List.of(arg0)));
        this.isSuccess = new SingleUse<>(true);
    }
}
