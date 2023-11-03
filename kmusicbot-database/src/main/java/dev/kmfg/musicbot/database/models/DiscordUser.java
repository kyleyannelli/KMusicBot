package dev.kmfg.musicbot.database.models;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "discord_users")
public class DiscordUser extends BaseKMusicTable {
	@Id
	@Column(name = "discord_id")
	private long discordId;

	@Column(name = "username")
	private String username;

	@OneToMany(mappedBy = "initializingDiscordUser")
	Set<SongInitialization> songInitializations = new HashSet<>();

	@OneToMany(mappedBy = "listeningDiscordUser")
	Set<SongPlaytime> songPlaytimes = new HashSet<>();

	@OneToMany(mappedBy = "usedByDiscordUser")
	Set<UsedCommand> usedCommands = new HashSet<>();

	public DiscordUser() {

	}

	public DiscordUser(long discordId, String username) {
		this.discordId = discordId;
		this.username = username;
	}

	public long getDiscordId() {
		return discordId;
	} 

	public String getDiscordUsername() {
		return username;
	}	
}
