package dev.kmfg.musicbot.api.stateless.models;

import com.google.gson.annotations.Expose;

import balbucio.discordoauth.model.User;

public class UserMetrics {
    @Expose
    private final long DISCORD_ID;
    @Expose
    private final String DISCORD_USERNAME;
    @Expose
    private final String DISCORD_AVATAR;
    @Expose
    private final long TOTAL_LISTEN_SECONDS;
    @Expose
    private final long TOTAL_INITIALIZATIONS;

    public UserMetrics(User discordApiUser, long totalListenSeconds, long totalInitializations) {
        this.DISCORD_ID = Long.valueOf(discordApiUser.getId());
        this.DISCORD_USERNAME = discordApiUser.getUsername();
        this.DISCORD_AVATAR = discordApiUser.getAvatar();
        this.TOTAL_LISTEN_SECONDS = totalListenSeconds;
        this.TOTAL_INITIALIZATIONS = totalInitializations;
    }

    public long getTotalListenSeconds() {
        return this.TOTAL_LISTEN_SECONDS;
    }

    public long getTotalInitializations() {
        return this.TOTAL_INITIALIZATIONS;
    }
}

