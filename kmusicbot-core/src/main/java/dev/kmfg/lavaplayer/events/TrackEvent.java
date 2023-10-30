package dev.kmfg.lavaplayer.events;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import dev.kmfg.sessions.AudioSession;

public abstract class TrackEvent {
    private final AudioSession audioSession;
    private final AudioTrack audioTrack;

    public TrackEvent(AudioSession audioSession, AudioTrack audioTrack) {
        this.audioSession = audioSession;
        this.audioTrack = audioTrack;
    }

    public AudioSession getAudioSession() {
        return this.audioSession;
    }

    public AudioTrack getAudioTrack() {
        return this.audioTrack;
    }
}
