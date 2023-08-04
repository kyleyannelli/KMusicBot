package dev.kmfg.lavaplayer.audioresulthandlers;

import dev.kmfg.exceptions.AlreadyAccessedException;
import dev.kmfg.helpers.SingleUse;
import dev.kmfg.lavaplayer.ProperTrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.ArrayList;

public abstract class KAudioResultHandler implements AudioLoadResultHandler {
    ProperTrackScheduler trackScheduler;
    SingleUse<Boolean> isSuccess;
    SingleUse<ArrayList<AudioTrack>> lastLoadedTracks;
    protected boolean deprioritizeQueue;
    protected boolean playNext;

    public KAudioResultHandler(ProperTrackScheduler trackScheduler) {
        this.trackScheduler = trackScheduler;
        // by default do not deprioritize
        this.deprioritizeQueue = false;
        this.playNext = false;
    }

    public ArrayList<AudioTrack> getLastLoadedTracks() throws AlreadyAccessedException {
        return lastLoadedTracks.get();
    }

    public boolean getIsSuccess() throws AlreadyAccessedException {
        return isSuccess.get();
    }

    public void deprioritizeQueue() {
        this.deprioritizeQueue = true;
    }

    public void prioritizeQueue() {
        this.deprioritizeQueue = false;
    }

    public void setPlayNext(boolean playNext) {
        this.playNext = playNext;
    }
}
