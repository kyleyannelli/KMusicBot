package dev.kmfg.musicbot.core.events;

import dev.kmfg.musicbot.core.sessions.AudioSession;

import org.javacord.api.entity.user.User;

import dev.kmfg.musicbot.core.lavaplayer.AudioTrackWithUser;

public class TrackEndIndividualEvent extends TrackEvent {
    private final User user;

    public TrackEndIndividualEvent(AudioSession audioSession, AudioTrackWithUser audioTrackWithUser, User user) {
        super(audioSession, audioTrackWithUser);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
