package dev.kmfg.musicbot.database.models;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.annotations.Expose;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "discord_guilds")
public class DiscordGuild extends BaseKMusicTable {
	@Id
	@Column(name = "discord_id")
    @Expose
	private long discordId;

	@OneToMany(mappedBy = "discordGuild")
	Set<TrackedSong> trackedSongs = new HashSet<>();

	@OneToMany(mappedBy = "discordGuild")
	Set<UsedCommand> usedCommands = new HashSet<>();

	public DiscordGuild() {

	}

	public DiscordGuild(long discordId) {
		this.discordId = discordId;
	}

	public long getDiscordId() {
		return this.discordId;
	}

    public Set<TrackedSong> getTrackedSongs() {
        return this.trackedSongs;
    }
}
