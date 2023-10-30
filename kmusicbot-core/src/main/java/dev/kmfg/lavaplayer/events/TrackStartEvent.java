package dev.kmfg.lavaplayer.events;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import dev.kmfg.sessions.AudioSession;

public class TrackStartEvent extends TrackEvent {
    public TrackStartEvent(AudioSession audioSession, AudioTrack audioTrack) {
        super(audioSession, audioTrack);
    }

}
