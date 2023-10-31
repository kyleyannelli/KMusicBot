package dev.kmfg.lavaplayer.events;

import dev.kmfg.lavaplayer.AudioTrackWithUser;
import dev.kmfg.sessions.AudioSession;

public abstract class TrackEvent {
    private final AudioSession audioSession;
    private final AudioTrackWithUser audioTrackWithUser;

    public TrackEvent(AudioSession audioSession, AudioTrackWithUser audioTrackWithUser) {
        this.audioSession = audioSession;
        this.audioTrackWithUser = audioTrackWithUser;
    }

    public AudioSession getAudioSession() {
        return this.audioSession;
    }

    public AudioTrackWithUser getAudioTrackWithUser() {
        return this.audioTrackWithUser;
    }
}
