package dev.kmfg.lavaplayer.audioresulthandlers;

import dev.kmfg.helpers.SingleUse;
import dev.kmfg.lavaplayer.ProperTrackScheduler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.tinylog.Logger;

import java.util.ArrayList;

public class SingleAudioResultHandler extends KAudioResultHandler {
    public SingleAudioResultHandler(ProperTrackScheduler trackScheduler) {
        super(trackScheduler);
    }
    @Override
    public void loadFailed(FriendlyException arg0) {
        Logger.error(arg0, "Track failed to load!");
        isSuccess = new SingleUse<>(false);
    }

    @Override
    public void noMatches() {
        Logger.warn("No matches found for query!");
        isSuccess = new SingleUse<>(false);
    }

    @Override
    public void playlistLoaded(AudioPlaylist arg0) {
        ArrayList<AudioTrack> loadedTracks = new ArrayList<>();
        // if theres a list of tracks AND first result, play the track
        if(arg0.getTracks() != null && arg0.getTracks().get(0) != null) {
            trackScheduler.loadSingleTrack(arg0.getTracks().get(0));
            loadedTracks.add(arg0.getTracks().get(0));
            isSuccess = new SingleUse<>(true);
        }
        lastLoadedTracks = new SingleUse<>(loadedTracks);
    }

    @Override
    public void trackLoaded(AudioTrack arg0) {
        trackScheduler.loadSingleTrack(arg0);
        isSuccess = new SingleUse<>(true);
        ArrayList<AudioTrack> loadedTracks = new ArrayList<>();
        loadedTracks.add(arg0);
        lastLoadedTracks = new SingleUse<>(loadedTracks);
    }
}
