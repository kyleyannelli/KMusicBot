package dev.kmfg.musicbot.api.stateless.models;

import com.google.gson.annotations.Expose;

public class DiscordGuildMetrics {
    @Expose
    private final long trackedSongCount;
    @Expose
    private final long totalPlaytimeSeconds;

    public DiscordGuildMetrics(long trackedSongCount, long totalPlaytimeSeconds) {
        this.totalPlaytimeSeconds = totalPlaytimeSeconds;
        this.trackedSongCount = trackedSongCount;
    }
}
