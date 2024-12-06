package dev.kmfg.musicbot.api.sockets;

import java.time.Instant;

import com.google.gson.annotations.Expose;

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

    @Expose
    public final long receivedAtEpochMs;

    public NowPlayingResponse(long guildId, String youtubeUri, String title, String author, long positionMs,
            long lengthMs) {
        this.guildId = guildId;
        this.youtubeUri = youtubeUri;
        this.title = title;
        this.author = author;
        this.positionMs = positionMs;
        this.lengthMs = lengthMs;
        this.receivedAtEpochMs = Instant.now().toEpochMilli();
    }
}
