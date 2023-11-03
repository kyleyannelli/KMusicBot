package dev.kmfg.musicbot.core.events;

import dev.kmfg.musicbot.core.sessions.AudioSession;
import dev.kmfg.musicbot.core.lavaplayer.AudioTrackWithUser;

public class TrackStartEvent extends TrackEvent {
    public TrackStartEvent(AudioSession audioSession, AudioTrackWithUser audioTrackWithUser) {
        super(audioSession, audioTrackWithUser);
    }

}
