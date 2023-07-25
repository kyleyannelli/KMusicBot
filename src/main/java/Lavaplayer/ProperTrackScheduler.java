package Lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import org.tinylog.Logger;

import java.util.*;

public class ProperTrackScheduler extends AudioEventAdapter {
    private final AudioPlayer audioPlayer;
    public ArrayList<AudioTrack> audioQueue = new ArrayList<>();
    public AudioTrack lastTrack;
    

    
    public ProperTrackScheduler(AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
        this.audioPlayer.addListener(this);
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
        if(lastTrack == null) lastTrack = track.makeClone();
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // log track end reason
        Logger.info("Track ended due to " + endReason.toString());
        // arraylist to iterator
        Iterator<AudioTrack> iterator = audioQueue.iterator();
        // Start next track
        if(iterator.hasNext()) {
            AudioTrack nextTrack = iterator.next();
            audioQueue.remove(nextTrack);
            this.audioPlayer.playTrack(nextTrack);
        }
        lastTrack = track.makeClone();
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        Logger.error(exception, "Track unexceptedly stopped.");
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        Logger.warn("AudioTrack " + track.getInfo().uri + " is stuck!. Stopping now...");
        // Audio track has been unable to provide us any audio, might want to just start a new track
        player.stopTrack();
    }

    public void loadSingleTrack(AudioTrack track) {
        if(this.audioPlayer.getPlayingTrack() == null) {
            this.audioPlayer.playTrack(track);
        } else {
            this.queue(track);
        }
    }

    public boolean loadPlaylist(AudioPlaylist audioPlaylist) {
        boolean isSuccess = audioPlaylist != null && audioPlaylist.getTracks() != null; 
        if(isSuccess) {
            for(AudioTrack audioTrack : audioPlaylist.getTracks()) {
                loadSingleTrack(audioTrack);
            }
        }
        return isSuccess;
    }

    private void queue(AudioTrack track) {
        audioQueue.add(track);
    }

    public void queueNext(AudioTrack track) {
        ArrayList<AudioTrack> newAudioQueue = new ArrayList<>();
        newAudioQueue.add(track);
        newAudioQueue.addAll(audioQueue);
        audioQueue = newAudioQueue;
    }
}
