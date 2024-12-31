package dev.kmfg.musicbot.core.lavaplayer;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

/**
 * DTO to help determine statistics for a
 * {@link dev.kmfg.musicbot.database.models.TrackedSong}.
 */
public class PositionalAudioTrack {
    private final boolean isQueuedByUser;
    private final AudioTrack audioTrack;
    private final int position;

    public PositionalAudioTrack(AudioTrack audioTrack, boolean isQueuedByUser, int position) {
        this.audioTrack = audioTrack;
        this.isQueuedByUser = isQueuedByUser;
        this.position = position;
    }

    public int getPosition() {
        return this.position;
    }

    public AudioTrack getAudioTrack() {
        return this.audioTrack;
    }

    public boolean isQueuedByUser() {
        return this.isQueuedByUser;
    }
}
