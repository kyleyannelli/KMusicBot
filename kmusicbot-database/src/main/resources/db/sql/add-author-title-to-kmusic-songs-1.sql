ALTER TABLE `kmusic_songs`
ADD COLUMN `author` varchar(50) AFTER `youtube_url`,
ADD COLUMN `title` varchar(75) AFTER `youtube_url`;
