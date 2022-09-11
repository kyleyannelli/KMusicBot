package Lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.audio.AudioSourceBase;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;

import java.util.*;

public class LavaplayerAudioSource extends AudioSourceBase {

    private final AudioPlayer audioPlayer;
    private AudioFrame lastFrame;
    private static HashMap<Long, AudioPlayer> players = new HashMap<>();
    private static HashMap<Long, TrackScheduler> schedulers = new HashMap<>();
    /**
     * Creates a new lavaplayer audio source.
     *
     * @param api A discord api instance.
     * @param audioPlayer An audio player from Lavaplayer.
     */
    public LavaplayerAudioSource(DiscordApi api, AudioPlayer audioPlayer) {
        super(api);
        this.audioPlayer = audioPlayer;
    }

    public static void removePlayerByServerId(long id) {
        players.remove(id);
    }

    @Override
    public byte[] getNextFrame() {
        if (lastFrame == null) {
            return null;
        }
        return applyTransformers(lastFrame.getData());
    }

    @Override
    public boolean hasFinished() {
        return false;
    }

    @Override
    public boolean hasNextFrame() {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Override
    public AudioSource copy() {
        return new LavaplayerAudioSource(getApi(), audioPlayer);
    }

    public static void setupAudioPlayer(DiscordApi api, AudioConnection audioConnection, String url, SlashCommandCreateEvent event) {
        // check if it's a YouTube link or search query
        AudioPlayerManager playerManager = createYouTubePlayerManager();
        if(url.startsWith("https://www.youtube.com/")) {
            // Create a player manager
            long serverId;
            try {
                serverId = event.getSlashCommandInteraction().getServer().get().getId();
            }
            catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return;
            }
            audioConnection.setAudioSource(updatePlayerAndCreateAudioSource(api, playerManager, serverId));
            // Load the track
            playerManager.loadItem(url, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    if(players.get(serverId).getPlayingTrack() == null) {
                        players.get(serverId).playTrack(track);
                        event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent("***Now Playing:*** \n" + track.getInfo().title + "\n" + track.getInfo().uri)
                                .send();
                    } else {
                        schedulers.get(serverId).queue(track);
                        event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent("***Queued:*** \n" + track.getInfo().title + "\n" + track.getInfo().uri)
                                .send();
                    }
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    if(players.get(serverId).getPlayingTrack() == null) {
                        for(int i = 1; i < playlist.getTracks().size(); i++) {
                            schedulers.get(serverId).queue(playlist.getTracks().get(i));
                        }
                        players.get(serverId).playTrack(playlist.getTracks().get(0));
                        event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent(
                                        "***Now Playing:*** \n" + playlist.getTracks().get(0).getInfo().title + "\n" + playlist.getTracks().get(0).getInfo().uri + "\n" +
                                        "***Queued " + (schedulers.get(serverId).audioQueue.size() - 1) + " Tracks...*** \n" + url)
                                .send();
                    } else {
                        for(AudioTrack track : playlist.getTracks()) {
                            schedulers.get(serverId).queue(track);
                        }
                        event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent(
                                                "***Queued " + (schedulers.get(serverId).audioQueue.size() - 1) + " Tracks...*** \n" + url)
                                .send();
                    }
                }

                @Override
                public void noMatches() {
                    // Notify the user that we've got nothing
                    event.getSlashCommandInteraction().createFollowupMessageBuilder()
                            .setContent("Nothing found by " + url)
                            .send();
                }

                @Override
                public void loadFailed(FriendlyException exception) {
                    // Notify the user that everything exploded
                    event.getSlashCommandInteraction().createFollowupMessageBuilder()
                            .setContent("Could not play: " + exception.getMessage())
                            .send();
                }
            });
        }
        else {
            // Create a player manager
            long serverId;
            try {
                serverId = event.getSlashCommandInteraction().getServer().get().getId();
            }
            catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return;
            }
            audioConnection.setAudioSource(updatePlayerAndCreateAudioSource(api, playerManager, serverId));
            // Load the track
            playerManager.loadItem("ytsearch:" + url, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    if(players.get(serverId).getPlayingTrack() == null) {
                        players.get(serverId).playTrack(track);
                        event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent("***Now Playing:*** \n" + track.getInfo().title + "\n" + track.getInfo().uri)
                                .send();
                    } else {
                        schedulers.get(serverId).queue(track);
                        event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent("***Queued:*** \n" + track.getInfo().title + "\n" + track.getInfo().uri)
                                .send();
                    }
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    if(players.get(serverId).getPlayingTrack() == null) {
                        players.get(serverId).playTrack(playlist.getTracks().get(0));
                        event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent("***Now Playing:*** \n" + playlist.getTracks().get(0).getInfo().title + "\n" + playlist.getTracks().get(0).getInfo().uri)
                                .send();
                    } else {
                        schedulers.get(serverId).queue(playlist.getTracks().get(0));
                        event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent("***Queued:*** \n" + playlist.getTracks().get(0).getInfo().title + "\n" + playlist.getTracks().get(0).getInfo().uri)
                                .send();
                    }
                }

                @Override
                public void noMatches() {
                    // Notify the user that we've got nothing
                    event.getSlashCommandInteraction().createFollowupMessageBuilder()
                            .setContent("Nothing found by " + url)
                            .send();
                }

                @Override
                public void loadFailed(FriendlyException exception) {
                    // Notify the user that everything exploded
                    event.getSlashCommandInteraction().createFollowupMessageBuilder()
                            .setContent("Could not play: " + exception.getMessage())
                            .send();
                }
            });
        }
    }

    public static AudioPlayer getPlayerByServerId(Long serverId) {
        return players.get(serverId);
    }

    private static AudioSource updatePlayerAndCreateAudioSource(DiscordApi api, AudioPlayerManager playerManager, Long serverId) {
        if(!players.containsKey(serverId)) {
            players.put(serverId, playerManager.createPlayer());
            schedulers.put(serverId, new TrackScheduler());
            players.get(serverId).addListener(schedulers.get(serverId));
        }

        // Create an audio source and add it to the audio connection's queue
        return new LavaplayerAudioSource(api, players.get(serverId));
    }

    private static AudioPlayerManager createYouTubePlayerManager() {
        // Create a player manager
        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        return playerManager;
    }
}
