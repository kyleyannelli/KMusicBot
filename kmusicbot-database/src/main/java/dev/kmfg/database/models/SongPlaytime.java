package dev.kmfg.database.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "song_playtimes")
public class SongPlaytime extends BaseKMusicTable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@ManyToOne
	@JoinColumn(name = "discord_user_id")
	private DiscordUser listeningDiscordUser;

	@ManyToOne
	@JoinColumn(name = "tracked_song_id")
	private TrackedSong trackedSong;

	@Column(name = "seconds_listened")
	private int secondsListened;

	public SongPlaytime(DiscordUser listeningDiscordUser, TrackedSong trackedSong) {
		this.listeningDiscordUser = listeningDiscordUser;
		this.trackedSong = trackedSong;
	}

	public void incSecondsListened(int incBySeconds) {
		this.secondsListened += incBySeconds;
	}

	public int getSecondsListened() {
		return this.secondsListened;
	}

	public DiscordUser getListeningDiscordUser() {
		return this.listeningDiscordUser;
	}

	public TrackedSong getTrackedSong() {
		return this.trackedSong;
	}
}
