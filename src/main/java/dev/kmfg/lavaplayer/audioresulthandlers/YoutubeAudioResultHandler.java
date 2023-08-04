package dev.kmfg.lavaplayer.audioresulthandlers;

import dev.kmfg.helpers.SingleUse;
import dev.kmfg.lavaplayer.ProperTrackScheduler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
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
        isSuccess = new SingleUse<>(false);
    }

    @Override
    public void playlistLoaded(AudioPlaylist arg0) {
        boolean loadPlaylistResult = trackScheduler.loadPlaylist(arg0);
        isSuccess = new SingleUse<>(loadPlaylistResult);
    }

    @Override
    public void trackLoaded(AudioTrack arg0) {
        trackScheduler.loadSingleTrack(arg0);
        isSuccess = new SingleUse<>(true);
    }
}
