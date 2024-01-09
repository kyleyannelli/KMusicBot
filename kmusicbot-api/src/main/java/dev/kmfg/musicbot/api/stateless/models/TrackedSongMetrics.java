package dev.kmfg.musicbot.api.stateless.models;

import java.util.Set;

import dev.kmfg.musicbot.database.models.DiscordUser;

import com.google.gson.annotations.Expose;

public class TrackedSongMetrics {
    @Expose
    private final Set<DiscordUser> USERS_WHO_LISTENED;
    @Expose
    private final long TOTAL_LISTEN_SECONDS; 
    @Expose
    private final long TOTAL_INITIALIZATIONS;

    public TrackedSongMetrics(Set<DiscordUser> listeningUsers, long totalListenSeconds, long totalInits) {
        this.USERS_WHO_LISTENED = listeningUsers;
        this.TOTAL_LISTEN_SECONDS = totalListenSeconds;
        this.TOTAL_INITIALIZATIONS = totalInits;
    }
}

