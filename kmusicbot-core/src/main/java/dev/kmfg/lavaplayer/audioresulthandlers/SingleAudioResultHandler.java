package dev.kmfg.lavaplayer.audioresulthandlers;

import dev.kmfg.database.models.DiscordUser;
import dev.kmfg.helpers.sessions.SingleUse;
import dev.kmfg.lavaplayer.AudioTrackWithUser;
import dev.kmfg.lavaplayer.ProperTrackScheduler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.tinylog.Logger;

import java.util.ArrayList;

/**
 * Intended to solely load just one track. Whether it goes to trackLoaded or playlistLoaded, the first result is always queued.
 */
public class SingleAudioResultHandler extends KAudioResultHandler {
    public SingleAudioResultHandler(ProperTrackScheduler trackScheduler, DiscordUser discordUser) {
        super(trackScheduler, discordUser);
    }

    /**
     * Not much happens here, isSuccess will turn to false, and the error gets logged.
     * @param friendlyException the FriendlyException
     */
    @Override
    public void loadFailed(FriendlyException friendlyException) {
        Logger.error(friendlyException, "Track failed to load!");
        isSuccess = new SingleUse<>(false);
    }

    @Override
    public void noMatches() {
        Logger.warn("No matches found for query!");
        isSuccess = new SingleUse<>(false);
    }

    /**
     * Queues the first track available in the AudioPlaylist (if it exists)
     * isSuccess updates to true here
     * @param audioPlaylist the AudioPlaylist
     */
    @Override
    public void playlistLoaded(AudioPlaylist audioPlaylist) {
        ArrayList<AudioTrackWithUser> loadedTracks = new ArrayList<>();
        // if theres a list of tracks AND first result, play the track
        if(audioPlaylist.getTracks() != null && audioPlaylist.getTracks().get(0) != null) {
            AudioTrack audioTrack = audioPlaylist.getTracks().get(0);
            AudioTrackWithUser audioTrackWithUser = new AudioTrackWithUser(audioTrack, discordUser);
            if(this.playNext && this.trackScheduler.hasNowPlaying()) {
                trackScheduler.queueNext(audioTrackWithUser);
            }
            else {
                trackScheduler.loadSingleTrack(audioTrackWithUser, this.deprioritizeQueue);
            }
            loadedTracks.add(audioTrackWithUser);
            isSuccess = new SingleUse<>(true);
        }
        lastLoadedTracks = new SingleUse<>(loadedTracks);
    }

    /**
     * Queues the loaded track
     * isSuccess updates to true here.
     * @param audioTrack the AudioTrack
     */
    @Override
    public void trackLoaded(AudioTrack audioTrack) {
        AudioTrackWithUser audioTrackWithUser = new AudioTrackWithUser(audioTrack, discordUser);
        if(this.playNext) {
            trackScheduler.queueNext(audioTrackWithUser);
        }
        else {
            trackScheduler.loadSingleTrack(audioTrackWithUser, this.deprioritizeQueue);
        }
        isSuccess = new SingleUse<>(true);
        ArrayList<AudioTrackWithUser> loadedTracks = new ArrayList<>();
        loadedTracks.add(audioTrackWithUser);
        lastLoadedTracks = new SingleUse<>(loadedTracks);
    }
}
