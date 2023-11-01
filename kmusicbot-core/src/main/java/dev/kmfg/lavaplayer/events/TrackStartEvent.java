package dev.kmfg.lavaplayer.events;

import dev.kmfg.lavaplayer.AudioTrackWithUser;
import dev.kmfg.sessions.AudioSession;

public class TrackStartEvent extends TrackEvent {
    public TrackStartEvent(AudioSession audioSession, AudioTrackWithUser audioTrackWithUser) {
        super(audioSession, audioTrackWithUser);
    }

}
