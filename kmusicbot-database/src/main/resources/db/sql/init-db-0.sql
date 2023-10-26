CREATE TABLE `discord_guilds` (
  `discord_id` bigint UNIQUE PRIMARY KEY,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `discord_users` (
  `discord_id` bigint UNIQUE PRIMARY KEY,
  `username` varchar(39) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `kmusic_songs` (
  `id` integer PRIMARY KEY AUTO_INCREMENT,
  `youtube_url` varchar(100) UNIQUE NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `tracked_songs` (
  `id` integer PRIMARY KEY AUTO_INCREMENT,
  `discord_guild_id` bigint NOT NULL,
  `kmusic_song_id` integer NOT NULL,
  `seconds_played` integer NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE (`discord_guild_id`, `kmusic_song_id`)
);

CREATE TABLE `song_initializations` (
  `id` integer PRIMARY KEY AUTO_INCREMENT,
  `tracked_song_id` integer NOT NULL,
  `init_discord_user_id` bigint NOT NULL,
  `times_init` integer NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE (`tracked_song_id`, `init_discord_user_id`),
  FOREIGN KEY (`tracked_song_id`) REFERENCES `tracked_songs` (`id`),
  FOREIGN KEY (`init_discord_user_id`) REFERENCES `discord_users` (`discord_id`)
);

CREATE TABLE `songs_playtime` (
  `id` integer PRIMARY KEY AUTO_INCREMENT,
  `discord_user_id` bigint NOT NULL,
  `tracked_song_id` integer NOT NULL,
  `seconds_listened` integer NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE (`discord_user_id`, `tracked_song_id`)
);

CREATE TABLE `used_commands` (
  `id` integer PRIMARY KEY AUTO_INCREMENT,
  `name` varchar(15) NOT NULL,
  `discord_guild_id` bigint NOT NULL,
  `used_by_discord_user_id` bigint NOT NULL,
  `times_used` integer NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE (`discord_guild_id`, `used_by_discord_user_id`, `name`)
);

ALTER TABLE `tracked_songs` ADD FOREIGN KEY (`discord_guild_id`) REFERENCES `discord_guilds` (`discord_id`) ON DELETE CASCADE;

ALTER TABLE `tracked_songs` ADD FOREIGN KEY (`kmusic_song_id`) REFERENCES `kmusic_songs` (`id`);

ALTER TABLE `songs_playtime` ADD FOREIGN KEY (`discord_user_id`) REFERENCES `discord_users` (`discord_id`);

ALTER TABLE `songs_playtime` ADD FOREIGN KEY (`tracked_song_id`) REFERENCES `tracked_songs` (`id`);

ALTER TABLE `used_commands` ADD FOREIGN KEY (`discord_guild_id`) REFERENCES `discord_guilds` (`discord_id`) ON DELETE CASCADE;

ALTER TABLE `used_commands` ADD FOREIGN KEY (`used_by_discord_user_id`) REFERENCES `discord_users` (`discord_id`);
