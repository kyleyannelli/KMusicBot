package Lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import org.tinylog.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ProperTrackScheduler extends AudioEventAdapter {
    private static final int MAX_RETRIES = 2; // maximum amount of times a track will attempt to be played.
    private final long ASSOCIATED_SESSION_ID;
    private int currentRetries = 0;
    private final AudioPlayer audioPlayer;

    public BlockingQueue<AudioTrack> audioQueue;
    public AudioTrack lastTrack;

    public ProperTrackScheduler(AudioPlayer audioPlayer, long associatedSessionId) {
        this.audioPlayer = audioPlayer;
        this.audioPlayer.addListener(this);

        this.audioQueue = new LinkedBlockingQueue<>();

        this.ASSOCIATED_SESSION_ID = associatedSessionId;
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        // Player was paused
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        // Player was resumed
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        Logger.info(getSessionIdString() + " Starting track: \"" + track.getInfo().uri + "\"");
        if(lastTrack == null) lastTrack = track.makeClone();
        currentRetries = 0;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // log track end reason if it is not FINISHED
        if(!endReason.equals(AudioTrackEndReason.FINISHED)) {
            Logger.warn(getSessionIdString() + " Track \"" + track.getInfo().uri + "\" in server ended due to " + endReason.toString());
        }
        AudioTrack nextTrack = audioQueue.poll();
        if(nextTrack != null) this.audioPlayer.playTrack(nextTrack);
        // clone the track for the ability to replay
        lastTrack = track.makeClone();
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        Logger.error(exception, getSessionIdString() + " Track unexceptedly stopped.");
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        // Audio track has been unable to provide us any audio, might want to just start a new track
        player.stopTrack();

        if(currentRetries <= MAX_RETRIES) {
            currentRetries++;
            Logger.warn(getSessionIdString() + " AudioTrack " + track.getInfo().uri + " is stuck after " + thresholdMs + "ms!. Retry #" + currentRetries);
            AudioTrack retryTrack = track.makeClone();
            this.audioPlayer.playTrack(retryTrack);
        }
        else {
            Logger.warn(getSessionIdString() + " AudioTrack " + track.getInfo().uri + " is stuck after " + thresholdMs + "ms!. Maximum retries (" + MAX_RETRIES + ") has been reached! Moving onto next track (if it's queued).");
            currentRetries = 0;
            AudioTrack nextTrack = audioQueue.poll();
            if(nextTrack != null) this.audioPlayer.playTrack(nextTrack);
        }
    }

    public void loadSingleTrack(AudioTrack track) {
        if(this.audioPlayer.getPlayingTrack() == null) {
            this.audioPlayer.playTrack(track);
        } else {
            this.queue(track);
        }
    }

    public boolean loadPlaylist(AudioPlaylist audioPlaylist) {
        boolean isSuccess = audioPlaylist != null && audioPlaylist.getTracks() != null;
        if(isSuccess) {
            for(AudioTrack audioTrack : audioPlaylist.getTracks()) {
                loadSingleTrack(audioTrack);
            }
        }
        return isSuccess;
    }

    public long getAssociatedSessionId() {
        return ASSOCIATED_SESSION_ID;
    }

    private String getSessionIdString() {
        return "|| Session ID: " + ASSOCIATED_SESSION_ID + " ||";
    }

    private void queue(AudioTrack track) {
        audioQueue.add(track);
        Logger.info(getSessionIdString() + " added \"" + track.getInfo().uri + "\" to the queue.");
    }

    public void queueNext(AudioTrack track) {
        BlockingQueue<AudioTrack> newBlockingQueue = new LinkedBlockingQueue<>();
        newBlockingQueue.add(track);
        newBlockingQueue.addAll(this.audioQueue);
        audioQueue.clear();
        audioQueue.addAll(newBlockingQueue);
    }
}
