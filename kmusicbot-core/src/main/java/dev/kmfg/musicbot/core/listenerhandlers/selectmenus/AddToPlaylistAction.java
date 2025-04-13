package dev.kmfg.musicbot.core.listenerhandlers.selectmenus;

import dev.kmfg.musicbot.core.util.messages.MessageSender;
import dev.kmfg.musicbot.database.models.KMusicSong;
import dev.kmfg.musicbot.database.models.Playlist;
import dev.kmfg.musicbot.database.repositories.KMusicSongRepo;
import dev.kmfg.musicbot.database.repositories.PlaylistRepo;
import dev.kmfg.musicbot.database.util.HibernateUtil;

import java.util.Optional;

public class AddToPlaylistAction {
    public static void addLinkToPlaylist(MessageSender messageSender, long guildId, String playlistName, String youtubeUrl) {
        final PlaylistRepo playlistRepo = new PlaylistRepo(HibernateUtil.getSessionFactory());
        final KMusicSongRepo songRepo = new KMusicSongRepo(HibernateUtil.getSessionFactory());
        final Optional<Playlist> playlistOpt = playlistRepo.findByGuildAndName(guildId, playlistName);
        final Optional<KMusicSong> songOpt = songRepo.findByYoutubeUrl(youtubeUrl);

        if(playlistOpt.isEmpty() || songOpt.isEmpty()) {
            messageSender.sendNothingFoundEmbed();
            return;
        }

        final KMusicSong song = songOpt.get();
        Playlist playlist = playlistOpt.get();

        for(KMusicSong pSong : playlist.getSongs()) {
            if(pSong.getYoutubeUrl().equals(song.getYoutubeUrl())) {
                messageSender.sendPlaylistHasSong(playlist, song);
                return;
            }
        }

        playlist.addSong(song);
        playlist = playlistRepo.save(playlist).orElse(playlist);

        messageSender.sendSongAddedToPlaylist(playlist, song);
    }
}
