package dev.kmfg.musicbot.core.events;

import dev.kmfg.musicbot.core.sessions.AudioSession;
import dev.kmfg.musicbot.core.lavaplayer.AudioTrackWithUser;

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
