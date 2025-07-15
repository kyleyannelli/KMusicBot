CREATE TABLE `playlists` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `guild_id` bigint(20) NOT NULL,
    `name` varchar(30) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `guild_id` (`guild_id`,`name`)
);

CREATE TABLE `kmusic_songs_playlists` (
    `playlist_id` int(11) NOT NULL,
    `kmusic_song_id` int(11) NOT NULL,
    PRIMARY KEY (`playlist_id`,`kmusic_song_id`),
    KEY `kmusic_song_id` (`kmusic_song_id`),
    CONSTRAINT `kmusic_songs_playlists_ibfk_1` FOREIGN KEY (`playlist_id`) REFERENCES `playlists` (`id`),
    CONSTRAINT `kmusic_songs_playlists_ibfk_2` FOREIGN KEY (`kmusic_song_id`) REFERENCES `kmusic_songs` (`id`)
);
