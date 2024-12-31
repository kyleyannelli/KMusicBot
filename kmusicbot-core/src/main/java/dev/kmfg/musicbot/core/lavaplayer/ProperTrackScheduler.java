package dev.kmfg.musicbot.core.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import dev.kmfg.musicbot.core.events.TrackEndEvent;
import dev.kmfg.musicbot.core.events.TrackEndIndividualEvent;
import dev.kmfg.musicbot.core.events.TrackEvent;
import dev.kmfg.musicbot.core.events.TrackStartEvent;
import dev.kmfg.musicbot.core.events.TrackStartIndividualEvent;
import dev.kmfg.musicbot.core.events.TrackStatisticRecorder;
import dev.kmfg.musicbot.core.sessions.AudioSession;
import dev.kmfg.musicbot.database.models.DiscordUser;

import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Optional;

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

    public void trackIndividualEnd(org.javacord.api.entity.user.User user) {
        var playingTrack = this.audioPlayer.getPlayingTrack();
        if (playingTrack == null)
            return;

        TrackEvent trackEvent = new TrackEndIndividualEvent(
                this.audioSession,
                new AudioTrackWithUser(
                        playingTrack,
                        trackUserMap.get(playingTrack)),
                user);
        this.trackStatisticRecorder.onTrackEvent(trackEvent);
    }

    public void trackIndividualStart(org.javacord.api.entity.user.User user) {
        var playingTrack = this.audioPlayer.getPlayingTrack();
        if (playingTrack == null)
            return;

        TrackEvent trackEvent = new TrackStartIndividualEvent(
                this.audioSession,
                new AudioTrackWithUser(
                        playingTrack,
                        trackUserMap.get(playingTrack)),
                user);
        this.trackStatisticRecorder.onTrackEvent(trackEvent);
    }

    public void handleTrackStartStatistics(AudioTrack audioTrack) {
        TrackEvent trackEvent = new TrackStartEvent(this.audioSession,
                new AudioTrackWithUser(audioTrack, trackUserMap.get(audioTrack)));
        this.trackStatisticRecorder.onTrackEvent(trackEvent);
    }

    protected void handleTrackStart(AudioPlayer audioPlayer, AudioTrack audioTrack) {
        Logger.info("{} Starting track: \"{}\"", getSessionIdString(), audioTrack.getInfo());
        if (lastTrack == null)
            lastTrack = audioTrack.makeClone();
        currentRetries = 0;
    }

    protected void handleTrackEndStatistics(AudioTrack track) {
        TrackEvent trackEvent = new TrackEndEvent(this.audioSession,
                new AudioTrackWithUser(track, trackUserMap.get(track)));
        this.trackStatisticRecorder.onTrackEvent(trackEvent);
        trackUserMap.remove(track);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        this.handleTrackEndStatistics(track);
        // log track end reason if it is not FINISHED
        if (!endReason.equals(AudioTrackEndReason.FINISHED) && !endReason.equals(AudioTrackEndReason.STOPPED)) {
            Logger.warn("{} Track \"{}\" in server ended due to {}", getSessionIdString(), track.getInfo().uri,
                    endReason);
        }

        // first attempt to get the audioQueue head, if not present pull from the
        // recommender queue
        AudioTrack nextTrack = audioQueue.peek() == null ? recommenderAudioQueue.poll().getAudioTrack()
                : audioQueue.poll().getAudioTrack();
        if (nextTrack != null)
            this.audioPlayer.playTrack(nextTrack);
        // clone the track for the ability to replay
        lastTrack = track.makeClone();
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        this.handleTrackEndStatistics(track);
        Logger.error(exception, "{} Track unexpectedly stopped.", getSessionIdString());
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        // Audio track has been unable to provide us any audio, might want to just start
        // a new track
        player.stopTrack();

        if (currentRetries <= MAX_RETRIES) {
            currentRetries++;
            Logger.warn("{} AudioTrack {} is stuck after {}ms!. Retry #{}", getSessionIdString(), track.getInfo().uri,
                    thresholdMs, currentRetries);
            AudioTrack retryTrack = track.makeClone();
            this.audioPlayer.playTrack(retryTrack);
        } else {
            this.handleTrackEndStatistics(track);
            Logger.warn(
                    "{} AudioTrack {} is stuck after {}ms!. Maximum retries ({}) has been reached! Moving onto next track (if it's queued).",
                    getSessionIdString(), track.getInfo().uri, thresholdMs, MAX_RETRIES);
            currentRetries = 0;
            AudioTrack nextTrack = audioQueue.poll().getAudioTrack();
            if (nextTrack != null)
                this.audioPlayer.playTrack(nextTrack);
        }
    }

    public void clearQueue() {
        this.audioQueue = new LinkedBlockingQueue<>();
        this.recommenderAudioQueue = new LinkedBlockingQueue<>();
    }

    public boolean hasNowPlaying() {
        return this.audioPlayer.getPlayingTrack() != null;
    }

    public void loadSingleTrack(AudioTrackWithUser audioTrackWithUser, boolean deprioritizeQueue) {
        this.trackUserMap.put(audioTrackWithUser.getAudioTrack(), audioTrackWithUser.getDiscordUser());
        if (this.audioPlayer.getPlayingTrack() == null) {
            this.audioPlayer.playTrack(audioTrackWithUser.getAudioTrack());
        } else {
            this.queue(audioTrackWithUser, deprioritizeQueue);
        }
    }

    public boolean loadPlaylist(List<AudioTrackWithUser> audioTracks, boolean deprioritizeQueue, boolean playNext) {
        boolean isSuccess = audioTracks != null;
        if (isSuccess) {
            for (AudioTrackWithUser audioTrack : audioTracks) {
                if (playNext) {
                    queueNext(audioTrack);
                } else {
                    loadSingleTrack(audioTrack, deprioritizeQueue);
                }
            }
        }
        return isSuccess;
    }

    public Optional<AudioTrackWithUser> remove(int position) {
        position--;
        int i = 0;
        AudioTrackWithUser removedTrack = null;
        for (AudioTrackWithUser audioTrackWithUser : this.getFullAudioQueue()) {
            if (i == position) {
                removedTrack = audioTrackWithUser;
                break;
            }
            i++;
        }

        if (removedTrack != null && position < this.audioQueue.size()) {
            this.audioQueue.remove(removedTrack);
        } else if (removedTrack != null && position < this.getFullAudioQueue().size()) {
            this.recommenderAudioQueue.remove(removedTrack);
        } else if (removedTrack != null) {
            Logger.warn(
                    "Attempted to remove track, but position was not inbounds despite the track being found.This is likely a logic error!");
        } else {
            Logger.warn(
                    "Attempted to remove track, but track is null! Position was likely out of bounds!\n\tRequested Position: {} | Available Range: 0 through {}",
                    position, this.getFullAudioQueue().size() - 1);
        }

        return Optional.ofNullable(removedTrack);
    }

    public ArrayList<AudioTrackWithUser> getFullAudioQueue() {
        ArrayList<AudioTrackWithUser> combined = new ArrayList<>();

        if (this.audioQueue != null && this.recommenderAudioQueue != null) {
            combined = new ArrayList<>(this.audioQueue);
            combined.addAll(this.recommenderAudioQueue);
        } else if (this.audioQueue == null && this.recommenderAudioQueue != null) {
            Logger.warn(
                    "AudioQueue null, but RecommenderQueue NOT null in Session {}\n\tPossibly running past shutdown!",
                    this);
            combined.addAll(this.recommenderAudioQueue);
        } else if (this.audioQueue != null && this.recommenderAudioQueue == null) {
            Logger.warn(
                    "AudioQueue NOT null, but RecommenderQueue null in Session {}\n\tPossibly running past shutdown!",
                    this);
            combined.addAll(this.audioQueue);
        } else {
            Logger.warn("Both AudioQueue & RecommenderQueue are null in Session {}\n\tPossibly running past shutdown!",
                    this);
        }

        return combined;
    }

    /**
     * Converts the generic AudioTrack queues to PositionalAudioTracks
     * This includes the user added queue at the head, and the recommender queue at
     * the tail.
     * 
     * @return ArrayList of PositionalAudioTrack
     */
    public ArrayList<PositionalAudioTrack> getPositionalAudioQueue() {
        // preallocate ArrayList size
        // this usually wont change much, but with bigger queue sizes it might increase
        // performance
        int queueSizeTotal = this.audioQueue.size() + this.recommenderAudioQueue.size();
        ArrayList<PositionalAudioTrack> queuedTracks = new ArrayList<>(queueSizeTotal);

        int position = 1;
        for (AudioTrackWithUser audioTrackWithUser : this.audioQueue) {
            queuedTracks.add(
                    new PositionalAudioTrack(audioTrackWithUser.getAudioTrack(), true, position++));
        }

        for (AudioTrackWithUser audioTrackWithUser : this.recommenderAudioQueue) {
            queuedTracks.add(
                    new PositionalAudioTrack(audioTrackWithUser.getAudioTrack(), false, position++));
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

        // create new BlockingQueue to stuff the tracks in, then set it equal to the
        // current queue
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

        if (deprioritizeQueue) {
            recommenderAudioQueue.add(audioTrackWithUser);
            stringBuilder.append(" to the deprioritized queue");
        } else {
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

    public void shutdown() {
        this.audioPlayer.stopTrack();
        this.audioQueue = null;
        this.recommenderAudioQueue = null;
        this.trackUserMap = null;
        this.lastTrack = null;
        this.trackStatisticRecorder.shutdown();
    }
}
