CREATE TABLE `playlists` (
    `id` integer PRIMARY KEY AUTO_INCREMENT,
    `guild_id` bigint NOT NULL,
    `name` VARCHAR(30) NOT NULL,
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    UNIQUE (`guild_id`, `name`)
);

CREATE TABLE `kmusic_songs_playlists` (
    `playlist_id` integer NOT NULL,
    `kmusic_song_id` integer NOT NULL,
    PRIMARY KEY (`playlist_id`, `kmusic_song_id`),
    FOREIGN KEY (`playlist_id`) REFERENCES `playlists` (`id`),
    FOREIGN KEY (`kmusic_song_id`) REFERENCES `kmusic_songs` (`id`)
);
