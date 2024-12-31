package dev.kmfg.musicbot.core.websocket;

import com.google.gson.annotations.Expose;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import dev.kmfg.musicbot.core.sessions.AudioSession;

/**
 * Simple DTO for a track which is playing in an {@link AudioSession}.
 */
public class NowPlayingResponse {
    @Expose
    public final long guildId;

    @Expose
    public final String youtubeUri;

    @Expose
    public final String title;

    @Expose
    public final String author;

    @Expose
    public final long positionMs;

    @Expose
    public final long lengthMs;

    /**
     * Gets the current {@link AudioTrack} for the given {@link AudioSession}.
     * If there is no present current {@link AudioTrack}, all String fields are set
     * to "", and all numbers are set to -1.
     */
    public NowPlayingResponse(long guildId, AudioSession audioSession) {
        AudioTrack audioTrack = audioSession.getLavaSource().getCurrentPlayingAudioTrack();

        if (audioTrack == null) {
            this.guildId = -1;
            this.youtubeUri = "";
            this.title = "";
            this.author = "";
            this.positionMs = -1;
            this.lengthMs = -1;
        } else {
            this.guildId = guildId;
            AudioTrackInfo trackInfo = audioTrack.getInfo();
            this.youtubeUri = trackInfo.uri;
            this.title = trackInfo.title;
            this.author = trackInfo.author;
            this.positionMs = audioTrack.getPosition();
            this.lengthMs = audioTrack.getDuration();
        }
    }
}
