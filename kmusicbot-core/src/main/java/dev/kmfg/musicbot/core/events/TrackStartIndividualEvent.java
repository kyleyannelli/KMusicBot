package dev.kmfg.musicbot.core.events;

import dev.kmfg.musicbot.core.sessions.AudioSession;

import org.javacord.api.entity.user.User;

import dev.kmfg.musicbot.core.lavaplayer.AudioTrackWithUser;

public class TrackStartIndividualEvent extends TrackEvent {
    private final User user;

    public TrackStartIndividualEvent(AudioSession audioSession, AudioTrackWithUser audioTrackWithUser, User user) {
        super(audioSession, audioTrackWithUser);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
