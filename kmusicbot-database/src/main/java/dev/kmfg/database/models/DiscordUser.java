package dev.kmfg.database.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table( name = "discord_users" )
public class DiscordUser {
	@Id
	@Column( name = "id" )
	private long discordId;

	@Column( name = "discord_username" )
	private String discordUsername;

	// @OneToMany(mappedBy = "discord_user_id", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	// Set<KMusicSong> kmusicSongs;

	public DiscordUser(long discordId, String discordUsername) {
		this.discordId = discordId;
		this.discordUsername = discordUsername;
	}

	public long getDiscordId() {
		return discordId;
	} 

	public String getDiscordUsername() {
		return discordUsername;
	}
}
