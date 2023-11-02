package dev.kmfg.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import dev.kmfg.database.models.DiscordUser;
import dev.kmfg.lavaplayer.events.TrackEvent;
import dev.kmfg.lavaplayer.events.TrackStartEvent;
import dev.kmfg.lavaplayer.events.TrackStatisticRecorder;
import dev.kmfg.sessions.AudioSession;

import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class ProperTrackScheduler extends AudioEventAdapter {
    private static final int MAX_RETRIES = 2; // maximum amount of times a track will attempt to be played.
    private int currentRetries = 0;
    private final AudioSession audioSession;
    private final AudioPlayer audioPlayer;
    private final TrackStatisticRecorder trackStatisticRecorder;

    private BlockingQueue<AudioTrackWithUser> audioQueue;
    private BlockingQueue<AudioTrackWithUser> recommenderAudioQueue;
    private ConcurrentHashMap<AudioTrack, DiscordUser> trackUserMap;
    public AudioTrack lastTrack;

    public ProperTrackScheduler(AudioSession audioSession, AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
        this.audioPlayer.addListener(this);

        this.audioQueue = new LinkedBlockingQueue<>();
        this.recommenderAudioQueue = new LinkedBlockingQueue<>();
        this.trackUserMap = new ConcurrentHashMap<>();

        this.audioSession = audioSession;

        this.trackStatisticRecorder = new TrackStatisticRecorder(this.audioSession.getSessionFactory());
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
        this.handleTrackStart(player, track);
        this.handleTrackStartStatistics(track);
    }

    public void handleTrackStartStatistics(AudioTrack audioTrack) {
        TrackEvent trackEvent = new TrackStartEvent(this.audioSession, new AudioTrackWithUser(audioTrack, trackUserMap.get(audioTrack)));
        this.trackStatisticRecorder.onTrackEvent(trackEvent);
    }

    protected void handleTrackStart(AudioPlayer audioPlayer, AudioTrack audioTrack) {
        Logger.info(getSessionIdString() + " Starting track: \"" + audioTrack.getInfo().uri + "\"");
        if(lastTrack == null) lastTrack = audioTrack.makeClone();
        currentRetries = 0;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // log track end reason if it is not FINISHED
        if(!endReason.equals(AudioTrackEndReason.FINISHED) && !endReason.equals(AudioTrackEndReason.STOPPED)) {
            Logger.warn(getSessionIdString() + " Track \"" + track.getInfo().uri + "\" in server ended due to " + endReason.toString());
        }

        // first attempt to get the audioQueue head, if not present pull from the recommender queue
        AudioTrack nextTrack = audioQueue.peek() == null ? recommenderAudioQueue.poll().getAudioTrack() : audioQueue.poll().getAudioTrack();
        if(nextTrack != null) this.audioPlayer.playTrack(nextTrack);
        // clone the track for the ability to replay
        lastTrack = track.makeClone();
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        Logger.error(exception, getSessionIdString() + " Track unexceptedly stopped.");
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        // Audio track has been unable to provide us any audio, might want to just start a new track
        player.stopTrack();

        if(currentRetries <= MAX_RETRIES) {
            currentRetries++;
            Logger.warn(getSessionIdString() + " AudioTrack " + track.getInfo().uri + " is stuck after " + thresholdMs + "ms!. Retry #" + currentRetries);
            AudioTrack retryTrack = track.makeClone();
            this.audioPlayer.playTrack(retryTrack);
        }
        else {
            Logger.warn(getSessionIdString() + " AudioTrack " + track.getInfo().uri + " is stuck after " + thresholdMs + "ms!. Maximum retries (" + MAX_RETRIES + ") has been reached! Moving onto next track (if it's queued).");
            currentRetries = 0;
            AudioTrack nextTrack = audioQueue.poll().getAudioTrack();
            if(nextTrack != null) this.audioPlayer.playTrack(nextTrack);
        }
    }

    public boolean hasNowPlaying() {
        return this.audioPlayer.getPlayingTrack() != null;
    }

    public void loadSingleTrack(AudioTrackWithUser audioTrackWithUser, boolean deprioritizeQueue) {
        this.trackUserMap.put(audioTrackWithUser.getAudioTrack(), audioTrackWithUser.getDiscordUser());
        if(this.audioPlayer.getPlayingTrack() == null) {
            this.audioPlayer.playTrack(audioTrackWithUser.getAudioTrack());
        } else {
            this.queue(audioTrackWithUser, deprioritizeQueue);
        }
    }

    public boolean loadPlaylist(List<AudioTrackWithUser> audioTracks, boolean deprioritizeQueue, boolean playNext) {
        boolean isSuccess = audioTracks != null;
        if(isSuccess) {
            for(AudioTrackWithUser audioTrack : audioTracks) {
                if(playNext) {
                    queueNext(audioTrack);
                }
                else {
                    loadSingleTrack(audioTrack, deprioritizeQueue);
                }
            }
        }
        return isSuccess;
    }

    public ArrayList<AudioTrackWithUser> getFullAudioQueue() {
        ArrayList<AudioTrackWithUser> combined = new ArrayList<>(this.audioQueue);
        combined.addAll(this.recommenderAudioQueue);
        return combined;
    }

    /**
     * Converts the generic AudioTrack queues to PositionalAudioTracks
     * This includes the user added queue at the head, and the recommender queue at the tail.
     * @return ArrayList of PositionalAudioTrack
     */
    public ArrayList<PositionalAudioTrack> getPositionalAudioQueue() {
        // preallocate ArrayList size
        // this usually wont change much, but with bigger queue sizes it might increase performance
        int queueSizeTotal = this.audioQueue.size() + this.recommenderAudioQueue.size();
        ArrayList<PositionalAudioTrack> queuedTracks = new ArrayList<>(queueSizeTotal);

        int position = 1;
        for(AudioTrackWithUser audioTrackWithUser : this.audioQueue) {
            queuedTracks.add(
                    new PositionalAudioTrack(audioTrackWithUser.getAudioTrack(), true, position++)
            );
        }

        for(AudioTrackWithUser audioTrackWithUser : this.recommenderAudioQueue) {
            queuedTracks.add(
                    new PositionalAudioTrack(audioTrackWithUser.getAudioTrack(), false, position++)
            );
        }

        return queuedTracks;
    }

    public ArrayList<AudioTrackWithUser> getRecommenderQueue() {
        return new ArrayList<>(this.recommenderAudioQueue);
    }

    public ArrayList<AudioTrackWithUser> getUserQueue() {
        return new ArrayList<>(this.audioQueue);
    }

    public AudioSession getAudioSession() {
        return this.audioSession;
    }

    public long getAssociatedSessionId() {
        return this.audioSession.getSessionId();
    }

    public int shufflePriorityQueue() {
        // convert to arraylist for simple shuffle
        ArrayList<AudioTrackWithUser> priorityQueueList = new ArrayList<>(this.audioQueue);
        Collections.shuffle(priorityQueueList);

        // create new BlockingQueue to stuff the tracks in, then set it equal to the current queue
        BlockingQueue<AudioTrackWithUser> shufflePriorityQueue = new LinkedBlockingQueue<>();
        priorityQueueList.stream().forEach(shufflePriorityQueue::add);
        this.audioQueue = shufflePriorityQueue;

        return this.audioQueue.size();
    }

    private String getSessionIdString() {
        StringBuilder stringBuilder = new StringBuilder()
            .append("|| Session ID: ")
            .append(this.getAssociatedSessionId())
            .append(" ||");
        return stringBuilder.toString();
    }

    private void queue(AudioTrackWithUser audioTrackWithUser, boolean deprioritizeQueue) {
        StringBuilder stringBuilder = new StringBuilder()
            .append(getSessionIdString())
            .append(" ")
            .append(audioTrackWithUser.getDiscordUser().getDiscordUsername())
            .append("/")
            .append(audioTrackWithUser.getDiscordUser().getDiscordId())
            .append(" added ")
            .append(audioTrackWithUser.getAudioTrack().getInfo().uri);

        if(deprioritizeQueue) {
            recommenderAudioQueue.add(audioTrackWithUser);
            stringBuilder.append(" to the deprioritized queue");
        }
        else {
            audioQueue.add(audioTrackWithUser);
            stringBuilder.append(" to the priority queue");
        }

        this.trackUserMap.put(audioTrackWithUser.getAudioTrack(), audioTrackWithUser.getDiscordUser());

        Logger.info(stringBuilder.toString());
    }

    public void queueNext(AudioTrackWithUser audioTrackWithUser) {
        BlockingQueue<AudioTrackWithUser> newBlockingQueue = new LinkedBlockingQueue<>();
        newBlockingQueue.add(audioTrackWithUser);
        newBlockingQueue.addAll(this.audioQueue);
        audioQueue.clear();
        audioQueue.addAll(newBlockingQueue);
        this.trackUserMap.put(audioTrackWithUser.getAudioTrack(), audioTrackWithUser.getDiscordUser());
    }
}
