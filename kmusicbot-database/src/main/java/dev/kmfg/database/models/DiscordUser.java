package dev.kmfg.database.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table( name = "discord_users" )
public class DiscordUser extends BaseKMusicTable {
	@Id
	@Column( name = "discord_id" )
	private long discordId;

	@Column( name = "username" )
	private String username;

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
