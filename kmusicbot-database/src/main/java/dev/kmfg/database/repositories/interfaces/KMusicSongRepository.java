package dev.kmfg.database.repositories.interfaces;

import java.util.Optional;

import dev.kmfg.database.models.KMusicSong;

public interface KMusicSongRepository {
	Optional<KMusicSong> findById(long id);
	Optional<KMusicSong> findByYoutubeUrl(String youtubeUrl);
	Optional<KMusicSong> save(KMusicSong song);
}
