package dev.kmfg.musicbot.database.models;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;

public class KMusicBot {
    private final long DISCORD_ID;
    private final HashSet<Long> audioSessionIds;
    private final Timestamp lastContacted;

    public KMusicBot(long discordId) {
        this.DISCORD_ID = discordId;
        this.audioSessionIds = new HashSet<>();
        this.lastContacted = Timestamp.from(Instant.now());
    }

    /**
     * Intended to be used when updating the last time KMusicBot has responded
     */
    public void refreshLastContacted() {
        this.lastContacted.setTime(Instant.now().toEpochMilli());
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append(this.DISCORD_ID)
                .append(" | ")
                .append(" last contact at ")
                .append(this.lastContacted.toString())
                .append(" | ")
                .append(this.audioSessionIds.toArray().length)
                .append(" sessions")
                .toString();
    }
}
