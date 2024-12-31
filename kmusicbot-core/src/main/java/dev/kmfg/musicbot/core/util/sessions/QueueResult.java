package dev.kmfg.musicbot.core.util.sessions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;
import java.util.Optional;

/**
 * DTO to make results from the
 * {@link dev.kmfg.musicbot.core.lavaplayer.ProperTrackScheduler} easier to
 * communicate to the end user.
 */
public class QueueResult {
    private final boolean success;
    private final boolean willPlayNow;
    private final List<AudioTrack> queuedTracks;
    private final AudioTrack queuedTrack;

    /**
     * The QueueResult for a playlist of tracks.
     *
     * @param success      status of queuing.
     * @param willPlayNow
     * @param queuedTracks
     */
    public QueueResult(boolean success, boolean willPlayNow, List<AudioTrack> queuedTracks) {
        this.success = success;
        this.queuedTracks = queuedTracks;
        this.willPlayNow = willPlayNow;
        this.queuedTrack = null;
    }

    /**
     * The QueueResult for a single track.
     *
     * @param success     status of queuing.
     * @param willPlayNow
     * @param queuedTrack
     */
    public QueueResult(boolean success, boolean willPlayNow, AudioTrack queuedTrack) {
        this.success = success;
        this.queuedTracks = null;
        this.willPlayNow = willPlayNow;
        this.queuedTrack = queuedTrack;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public boolean willPlayNow() {
        return this.willPlayNow;
    }

    public Optional<List<AudioTrack>> getQueuedTracks() {
        return Optional.ofNullable(this.queuedTracks);
    }

    public Optional<AudioTrack> getQueueTrack() {
        return Optional.ofNullable(queuedTrack);
    }
}
