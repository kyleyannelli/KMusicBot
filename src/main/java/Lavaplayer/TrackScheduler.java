package Lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.*;

public class TrackScheduler extends AudioEventAdapter {
    public ArrayList<AudioTrack> audioQueue = new ArrayList<>();
    public TrackScheduler() {
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
