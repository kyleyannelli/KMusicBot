package dev.kmfg.musicbot.api.stateless.models;

import com.google.gson.annotations.Expose;

public class DiscordGuildIds {
    @Expose
    private final String name;
    @Expose
    private final String iconHash;
    @Expose
    private final long id;

    public DiscordGuildIds(String name, long id, String iconHash) {
        this.name = name;
        this.id = id;
        this.iconHash = iconHash;
    }
}
