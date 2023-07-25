package AudioResultHandlers;

import Exceptions.AlreadyAccessedException;
import Helpers.SingleUse;
import Lavaplayer.ProperTrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.ArrayList;

public abstract class KAudioResultHandler implements AudioLoadResultHandler {
    ProperTrackScheduler trackScheduler;
    SingleUse<Boolean> isSuccess;
    SingleUse<ArrayList<AudioTrack>> lastLoadedTracks;

    public KAudioResultHandler(ProperTrackScheduler trackScheduler) {
        this.trackScheduler = trackScheduler;
    }

    public ArrayList<AudioTrack> getLastLoadedTracks() throws AlreadyAccessedException {
        return lastLoadedTracks.get();
    }

    public boolean getIsSuccess() throws AlreadyAccessedException {
        return isSuccess.get();
    }
}
