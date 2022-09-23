package Lavaplayer;

import MySQL.Songs;
import MySQL.Users;
import SpotifyApi.HandleSpotifyLink;
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
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import se.michaelthelin.spotify.SpotifyApi;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LavaplayerAudioSource extends AudioSourceBase {

    private final AudioPlayer audioPlayer;
    private AudioFrame lastFrame;
    private static HashMap<Long, AudioPlayer> players = new HashMap<>();
    private static HashMap<Long, TrackScheduler> schedulers = new HashMap<>();
    public static HashMap<Long, Timer> timers = new HashMap<>();
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

    public static void shuffleQueue(Long serverId) {
        Collections.shuffle(schedulers.get(serverId).audioQueue);
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

    public static void setupAudioPlayer(DiscordApi api, SpotifyApi spotifyApi, AudioConnection audioConnection, String url, SlashCommandCreateEvent event) {
        if(timers.get(event.getSlashCommandInteraction().getServer().get().getId()) == null) {
            // get server id
            long serverId = event.getSlashCommandInteraction().getServer().get().getId();
            // start timer for 5 minutes
            createDisconnectTimer(api, serverId, new Timer());
        }
        else {
            // get the timer and reset it
            Timer timer = timers.get(event.getSlashCommandInteraction().getServer().get().getId());
            timer.cancel();
            timer.purge();
            // get server id
            long serverId = event.getSlashCommandInteraction().getServer().get().getId();
            createDisconnectTimer(api, serverId, new Timer());
        }
        // check if it's a YouTube link or search query
        AudioPlayerManager playerManager = createYouTubePlayerManager();
        if(url.startsWith("https://www.youtube.com/")) {
            // Create a player manager
            entirelyLoadTrack(api, playerManager, audioConnection, event, url, false);
        }
        else if(url.startsWith("https://open.spotify.com/")) {
            Collection<String> trackNames;
            try {
                trackNames = HandleSpotifyLink.getCollectionFromSpotifyLink(spotifyApi, url);
                if(trackNames == null) {
                    event.getSlashCommandInteraction().createFollowupMessageBuilder()
                            .setContent("Not a playlist link\nIf you think this is an error, please contact <@806350925723205642>")
                            .send();
                }
                else if(trackNames.size() == 0) {
                    event.getSlashCommandInteraction().createFollowupMessageBuilder()
                            .setContent("Not a playlist link\nIf you think this is an error, please contact <@806350925723205642>")
                            .send();
                }
            }
            catch (Exception e) {
                event.getSlashCommandInteraction().createFollowupMessageBuilder()
                        .setContent("Not a playlist link\nIf you think this is an error, please contact <@806350925723205642>")
                        .send();
                return;
            }
            try {
                setAudioPlayer(api, audioConnection, playerManager, event);
                long serverId = event.getSlashCommandInteraction().getServer().get().getId();
                for(String trackName : trackNames) {
                    playerManagerLoadTrack(playerManager, "ytsearch:" + trackName, event, serverId, false, true);
                }
                event.getSlashCommandInteraction().createFollowupMessageBuilder()
                        .setContent("Added " + trackNames.size() + " tracks to the queue")
                        .send();
            }
            catch (Exception e) {
                event.getSlashCommandInteraction().createFollowupMessageBuilder()
                        .setContent("An error occurred while adding the spotify tracks to the queue\nPlease try again.")
                        .send();
            }
        }
        else {
            entirelyLoadTrack(api, playerManager, audioConnection, event, "ytsearch:" + url, true);
        }
    }

    public static void createDisconnectTimer(DiscordApi api, long serverId, Timer timer) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(players.get(serverId).getPlayingTrack() == null) {
                    Server server = api.getServerById(serverId).get();
                    api.getYourself().getConnectedVoiceChannel(server).get().disconnect();
                    removePlayerByServerId(serverId);
                    timers.remove(serverId);
                }
                else {
                    createDisconnectTimer(api, serverId, new Timer());
                }
            }
        }, 300000);
        timers.put(serverId, timer);
    }

    public static AudioPlayer getPlayerByServerId(Long serverId) {
        return players.get(serverId);
    }

    private static AudioSource updatePlayerAndCreateAudioSource(DiscordApi api, AudioPlayerManager playerManager, Long serverId) {
        if(!players.containsKey(serverId)) {
            players.put(serverId, playerManager.createPlayer());
            schedulers.put(serverId, new TrackScheduler(serverId, api));
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

    private static void playerManagerLoadTrack(AudioPlayerManager playerManager, String url, SlashCommandCreateEvent event, long serverId, boolean sendFollowupMessage, boolean isSearch) {
        playerManager.loadItem(url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                long userId;
                try {
                    Songs s = new Songs(track.getInfo().title, track.getInfo().author);
                    long songId = s.save("" + serverId);
                    Users user = new Users(event.getSlashCommandInteraction().getUser().getId(), serverId, songId, 0);
                    userId = user.save("" + serverId);
                    schedulers.get(serverId).userDiscordIdRequestedSongId.put(track.getInfo().identifier, userId);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error while saving song to database, continuing...");
                }
                if(players.get(serverId).getPlayingTrack() == null) {
                    players.get(serverId).playTrack(track);
                    if(sendFollowupMessage) {
                        event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent("***Now Playing:*** \n" + track.getInfo().title + "\n<" + track.getInfo().uri + ">")
                                .send();
                    }
                } else {
                    schedulers.get(serverId).queue(track);
                    if(sendFollowupMessage) {
                        event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent("***Queued:*** \n" + track.getInfo().title + "\n<" + track.getInfo().uri + ">")
                                .send();
                    }
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if(players.get(serverId).getPlayingTrack() == null) {
                    if(isSearch) {
                        // play the first track
                        players.get(serverId).playTrack(playlist.getTracks().get(0));
                        long userId;
                        try {
                            Songs s = new Songs(playlist.getTracks().get(0).getInfo().title, playlist.getTracks().get(0).getInfo().author, playlist.getTracks().get(0).getInfo().uri);
                            long songId = s.save("" + serverId);
                            System.out.println("songId: " + songId);
                            Users user = new Users(event.getSlashCommandInteraction().getUser().getId(), serverId, songId, 0);
                            userId = user.save("" + serverId);
                            schedulers.get(serverId).userDiscordIdRequestedSongId.put(playlist.getTracks().get(0).getInfo().identifier, userId);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Error while saving song to database, continuing...");
                        }
                        if(sendFollowupMessage) {
                            event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                    .setContent("***Now Playing:*** \n" + playlist.getTracks().get(0).getInfo().title + "\n<" + playlist.getTracks().get(0).getInfo().uri + ">")
                                    .send();
                        }
                    }
                    else {
                        for(int i = 1; i < playlist.getTracks().size(); i++) {
                            schedulers.get(serverId).queue(playlist.getTracks().get(i));
                        }
                        players.get(serverId).playTrack(playlist.getTracks().get(0));
                        if(sendFollowupMessage) {
                            event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                    .setContent(
                                            "***Now Playing:*** \n" + playlist.getTracks().get(0).getInfo().title + "\n<" + playlist.getTracks().get(0).getInfo().uri + ">\n" + "***Queued " + (schedulers.get(serverId).audioQueue.size() - 1) + " Tracks...*** \n" + url)
                                    .send();
                        }
                    }
                } else {
                    if(isSearch) {
                        long userId;
                        try {
                            Songs s = new Songs(playlist.getTracks().get(0).getInfo().title, playlist.getTracks().get(0).getInfo().author, playlist.getTracks().get(0).getInfo().uri);
                            long songId = s.save("" + serverId);
                            Users user = new Users(event.getSlashCommandInteraction().getUser().getId(), serverId, songId, 0);
                            userId = user.save("" + serverId);
                            schedulers.get(serverId).userDiscordIdRequestedSongId.put(playlist.getTracks().get(0).getInfo().identifier, userId);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Error while saving song to database, continuing...");
                        }
                        // play the first track
                        schedulers.get(serverId).queue(playlist.getTracks().get(0));
                        if(sendFollowupMessage) {
                            event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                    .setContent("***Queued:*** \n" + playlist.getTracks().get(0).getInfo().title + "\n<" + playlist.getTracks().get(0).getInfo().uri + ">")
                                    .send();
                        }
                    }
                    else {
                        for(AudioTrack track : playlist.getTracks()) {
                            long userId;
                            try {
                                Songs s = new Songs(track.getInfo().title, track.getInfo().author, track.getInfo().uri);
                                long songId = s.save("" + serverId);
                                Users user = new Users(event.getSlashCommandInteraction().getUser().getId(), serverId, songId, 0);
                                userId = user.save("" + serverId);
                                schedulers.get(serverId).userDiscordIdRequestedSongId.put(track.getInfo().identifier, userId);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                System.out.println("Error while saving song to database, continuing...");
                            }
                            schedulers.get(serverId).queue(track);
                        }
                        if(sendFollowupMessage) {
                            event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                    .setContent(
                                            "***Queued " + (schedulers.get(serverId).audioQueue.size() - 1) + " Tracks...*** \n<" + url + ">")
                                    .send();
                        }
                    }
                }
            }

            @Override
            public void noMatches() {
                if(sendFollowupMessage) {
                    // Notify the user that we've got nothing
                    event.getSlashCommandInteraction().createFollowupMessageBuilder()
                            .setContent("Nothing found by <" + url + ">")
                            .send();
                }
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                if(sendFollowupMessage) {
                    // Notify the user that everything exploded
                    event.getSlashCommandInteraction().createFollowupMessageBuilder()
                            .setContent("Could not play: " + exception.getMessage())
                            .send();
                }
            }
        });
    }

    private static void entirelyLoadTrack(DiscordApi api, AudioPlayerManager playerManager, AudioConnection audioConnection, SlashCommandCreateEvent event, String url, boolean isSearch) {
        setAudioPlayer(api, audioConnection, playerManager, event);
        // Load the track
        playerManagerLoadTrack(playerManager, url, event, event.getSlashCommandInteraction().getServer().get().getId(), true, isSearch);
    }

    private static void setAudioPlayer(DiscordApi api, AudioConnection audioConnection, AudioPlayerManager playerManager, SlashCommandCreateEvent event) {
        long serverId;
        try {
            serverId = event.getSlashCommandInteraction().getServer().get().getId();
        }
        catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }
        audioConnection.setAudioSource(updatePlayerAndCreateAudioSource(api, playerManager, serverId));
    }

    public static String getQueue(long serverId, long pageNumber) {
        if(pageNumber < 1) {
            pageNumber = 1;
        }
        pageNumber--;
        // show 5 tracks per page
        StringBuilder queue = new StringBuilder();
        try {
            int start = (int) (pageNumber * 5);
            int end = (start + 5);
            // if start is greater than queue size show last page
            if(start > schedulers.get(serverId).audioQueue.size()) {
                start = schedulers.get(serverId).audioQueue.size() - 5;
                end = schedulers.get(serverId).audioQueue.size();
            }
            if(start > schedulers.get(serverId).audioQueue.size()) {
                return "No tracks in queue.";
            }
            if(end > schedulers.get(serverId).audioQueue.size()) {
                end = schedulers.get(serverId).audioQueue.size();
            }
            for(int i = start; i < end; i++) {
                queue.append(i + 1).append(". ").append(schedulers.get(serverId).audioQueue.get(i).getInfo().title).append("\n");
            }
        }
        catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return "There was an error getting the queue.";
        }
        return queue.toString();
    }

    public static boolean pause(long serverId) {
        if(players.get(serverId).isPaused()) {
            players.get(serverId).setPaused(false);
            return false;
        }
        else {
            players.get(serverId).setPaused(true);
            return true;
        }
    }
}
