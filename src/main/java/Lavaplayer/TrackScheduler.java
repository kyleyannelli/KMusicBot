package Lavaplayer;

import MySQL.Songs;
import MySQL.Users;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import org.javacord.api.DiscordApi;

import java.time.Instant;
import java.util.*;

public class TrackScheduler extends AudioEventAdapter {
    public long lastSongStartTime;
    public HashMap<String, Long> userDiscordIdRequestedSongId = new HashMap<>();
    public ArrayList<AudioTrack> audioQueue = new ArrayList<>();
    public long serverId;
    public AudioTrack lastTrack;
    public DiscordApi api;
    public TrackScheduler(long serverId, DiscordApi api) {
        this.serverId = serverId;
        this.api = api;
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        // Player was paused
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        // Player was resumed
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        // A track started playing
        lastSongStartTime = Instant.now().getEpochSecond();
        if(lastTrack == null) lastTrack = track.makeClone();
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        try {
            long songId = Songs.getSongIdByTrack(track, serverId);
            long timeSpent = Instant.now().getEpochSecond() - lastSongStartTime;
            long userDiscordId = userDiscordIdRequestedSongId.get(track.getInfo().identifier);
            Users.addToTimeSpent(serverId, userDiscordId, songId, timeSpent);
        }
        catch (Exception e) {
            System.out.println("Error adding time spent");
            e.printStackTrace();
        }
        // arraylist to iterator
        Iterator<AudioTrack> iterator = audioQueue.iterator();
        // Start next track
        if(iterator.hasNext()) {
            AudioTrack nextTrack = iterator.next();
            audioQueue.remove(nextTrack);
            player.playTrack(nextTrack);
            if(LavaplayerAudioSource.timers.get(serverId) != null) {
                // start timer for 5 minutes
                LavaplayerAudioSource.createDisconnectTimer(api, serverId, new Timer());
            }
            else {
                // get the timer and reset it
                Timer timer = LavaplayerAudioSource.timers.get(serverId);
                timer.cancel();
                timer.purge();
                LavaplayerAudioSource.createDisconnectTimer(api, serverId, new Timer());
            }
        }
        lastTrack = track.makeClone();
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        // An already playing track threw an exception (track end event will still be received separately)
        try {
            long songId = Songs.getSongIdByTrack(track, serverId);
            long timeSpent = Instant.now().getEpochSecond() - lastSongStartTime;
            long userDiscordId = userDiscordIdRequestedSongId.get(track.getInfo().identifier);
            Users.addToTimeSpent(serverId, userDiscordId, songId, timeSpent);
        }
        catch (Exception e) {
            System.out.println("Error adding time spent");
            e.printStackTrace();
        }
        System.out.println(exception.getMessage());
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        // Audio track has been unable to provide us any audio, might want to just start a new track
        player.stopTrack();
        try {
            long songId = Songs.getSongIdByTrack(track, serverId);
            long timeSpent = Instant.now().getEpochSecond() - lastSongStartTime;
            long userDiscordId = userDiscordIdRequestedSongId.get(track.getInfo().identifier);
            Users.addToTimeSpent(serverId, userDiscordId, songId, timeSpent);
        }
        catch (Exception e) {
            System.out.println("Error adding time spent");
            e.printStackTrace();
        }
    }

    public void queue(AudioTrack track) {
        audioQueue.add(track);
    }

    public void queueNext(AudioTrack track) {
        ArrayList<AudioTrack> newAudioQueue = new ArrayList<>();
        newAudioQueue.add(track);
        newAudioQueue.addAll(audioQueue);
        audioQueue = newAudioQueue;
    }
}
