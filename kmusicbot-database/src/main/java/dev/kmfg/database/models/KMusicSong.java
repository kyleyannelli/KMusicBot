package dev.kmfg.database.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "kmusic_songs")
public class KMusicSong extends BaseKMusicTable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private int id;

	@Column(name = "youtube_url")
	private String youtubeUrl;

	public KMusicSong(int id, String youtubeUrl) {
		this.id = id;
		this.youtubeUrl = youtubeUrl;
	}

	public int getId() {
		return this.id;
	}

	public String getYoutubeUrl() {
		return this.youtubeUrl;
	}
}
