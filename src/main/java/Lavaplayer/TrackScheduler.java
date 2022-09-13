package Lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import org.javacord.api.DiscordApi;

import java.util.*;

public class TrackScheduler extends AudioEventAdapter {
    public ArrayList<AudioTrack> audioQueue = new ArrayList<>();
    public long serverId;
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
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
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
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        // An already playing track threw an exception (track end event will still be received separately)
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        // Audio track has been unable to provide us any audio, might want to just start a new track
        player.stopTrack();
    }

    public void queue(AudioTrack track) {
        audioQueue.add(track);
    }
}
