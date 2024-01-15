package dev.kmfg.musicbot.database.models;

import com.google.gson.annotations.Expose;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "song_initializations")
public class SongInitialization extends BaseKMusicTable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private int id;

	@ManyToOne
	@JoinColumn(name = "tracked_song_id")
	private TrackedSong trackedSong;

	@ManyToOne
	@JoinColumn(name = "init_discord_user_id")
    @Expose
	private DiscordUser initializingDiscordUser;

	@Column(name = "times_init", updatable = true)
    @Expose
	private int timesInitialized;

	public SongInitialization(TrackedSong trackedSong, DiscordUser initDiscordUser) {
		this.trackedSong = trackedSong;
		this.initializingDiscordUser = initDiscordUser;
	}

	public SongInitialization() {

	}

	public void incTimesInitialized() {
		this.timesInitialized++;
	}

	public int getTimesInitialized() {
		return this.timesInitialized;
	}

	public int getId() {
		return this.id;
	}

	public TrackedSong getTrackedSong() {
		return this.trackedSong;
	}

	public DiscordUser getInitDiscordUser() {
		return this.initializingDiscordUser;
	}
}

