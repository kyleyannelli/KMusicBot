package dev.kmfg.lavaplayer.events;

import dev.kmfg.lavaplayer.AudioTrackWithUser;
import dev.kmfg.sessions.AudioSession;

public class TrackEndEvent extends TrackEvent {
    public TrackEndEvent(AudioSession audioSession, AudioTrackWithUser audioTrackWithUser) {
        super(audioSession, audioTrackWithUser);
    }
}
