package dev.kmfg.database.models;

import java.util.HashSet;
import java.util.Set;

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
	private long discordId;

	@OneToMany(mappedBy = "discordGuild")
	Set<TrackedSong> trackedSongs = new HashSet<>();

	@OneToMany(mappedBy = "discordGuild")
	Set<UsedCommand> usedCommands = new HashSet<>();
}
