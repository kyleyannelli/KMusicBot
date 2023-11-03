package dev.kmfg.musicbot.database.models;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "tracked_songs")
public class TrackedSong extends BaseKMusicTable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private int id;

	@ManyToOne
	@JoinColumn(name = "discord_guild_id")
	private DiscordGuild discordGuild;

	@ManyToOne
	@JoinColumn(name = "kmusic_song_id")
	private KMusicSong kmusicSong;

	@OneToMany(mappedBy = "trackedSong")
	Set<SongInitialization> songInitializations = new HashSet<>();

	@OneToMany(mappedBy = "trackedSong")
	Set<SongPlaytime> songPlaytimes = new HashSet<>();

	@Column(name = "seconds_played")
	private int secondsPlayed;

	public TrackedSong() {

	}

	public TrackedSong(DiscordGuild discordGuild, KMusicSong kmusicSong) {
		this.discordGuild = discordGuild;
		this.kmusicSong = kmusicSong;
	}

	public void incrementSecondsPlayed(int incBy) {
		this.secondsPlayed += incBy;
	}

	public int getSecondsPlayed() {
		return this.secondsPlayed;
	}

	public DiscordGuild getGuild() {
		return this.discordGuild;
	}

	public KMusicSong getSong() {
		return this.kmusicSong;
	}

	public int getId() {
		return this.id;
	}
}

