package dev.kmfg.lavaplayer.events;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import dev.kmfg.sessions.AudioSession;

public class TrackEndEvent extends TrackEvent {
    public TrackEndEvent(AudioSession audioSession, AudioTrack audioTrack) {
        super(audioSession, audioTrack);
    }
}
