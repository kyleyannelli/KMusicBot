package dev.kmfg.musicbot.api.stateless.models;

import com.google.gson.annotations.Expose;

public class DiscordGuildUserPlaytimeMetric {
    @Expose
    private final String GUILD_NAME;
    @Expose
    private final long GUILD_ID;
    @Expose
    private final long LISTENED_SECONDS;
    @Expose
    private final long TIMES_INITIALIZED;

    public DiscordGuildUserPlaytimeMetric(String guildName, long guildId, long listenedSeconds, long timesInitialized) {
        this.GUILD_NAME = guildName;
        this.GUILD_ID = guildId;
        this.LISTENED_SECONDS = listenedSeconds;
        this.TIMES_INITIALIZED = timesInitialized;
    }
}
