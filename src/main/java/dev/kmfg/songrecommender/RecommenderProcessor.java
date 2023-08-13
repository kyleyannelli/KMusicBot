package dev.kmfg.songrecommender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.javacord.api.DiscordApi;
import org.tinylog.Logger;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import se.michaelthelin.spotify.SpotifyApi;

public class RecommenderProcessor {

    private final ExecutorService executorService;

    // Spotify and Discord API objects are persistent the entire application life time
    private final DiscordApi discordApi;
    private final SpotifyApi spotifyApi;

    private final RecommenderRequester recommenderRequester;

    private final ConcurrentHashMap<Long, List<FutureTask<Void>>> queuedTasksMap;

    public RecommenderProcessor(DiscordApi discordApi, SpotifyApi spotifyApi, int maxThreads) {
        this.executorService = Executors.newFixedThreadPool(maxThreads);

        this.discordApi = discordApi;
        this.spotifyApi = spotifyApi;

        this.recommenderRequester = new RecommenderRequester(spotifyApi);
        this.queuedTasksMap = new ConcurrentHashMap<>();

        // make sure to properly handle a shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.cancelAllTasks();
            this.shutdown();
        }));
    }

    public void shutdown() {
        // first shutdown this executorService
        this.executorService.shutdown();
        try {
            if(!this.executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                this.executorService.shutdownNow();
            }
        }
        catch(InterruptedException interruptedException) {
            this.executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // now shutdown the recommenderRequester
        recommenderRequester.shutdown();
    }

    public void addRecommendedSongsFromSpotify(RecommenderSession session) {
        Logger.info("RecommenderProcessor invoked from Session ID: " + session.getSessionId());
        Runnable runnable = () -> {
            String[] spotifyRecommendations = getRecommendationsFromSpotify(session);
            try {
                session.addRecommendationsToQueue(spotifyRecommendations);
            }
            catch(InterruptedException interruptedException) {
                Logger.error(interruptedException, "Failed to automatically add tracks to queue due to interrupt");
                Thread.currentThread().interrupt();
            }

            if(spotifyRecommendations.length == 0) {
                Logger.warn("Session " + session.getSessionId() + " belonging to server " + session.getAssociatedServerId() + " received a 0 length recommendation array. Spotify request likely failed!");
            }
            else {
                // setup logging info
                StringBuilder infoTextBuilder = new StringBuilder();
                infoTextBuilder.append("Added recommendations: ");
                for(String recommendation : spotifyRecommendations) infoTextBuilder.append("\n\t").append(recommendation);
                infoTextBuilder.append("\nTo the queue of ").append(session.getSessionId()).append(" belonging to server ").append(session.getAssociatedServerId());
                Logger.info(infoTextBuilder.toString());
            }
        };

        submitTask(session.getSessionId(), runnable);
    }

    public void cancelTasksBySessionId(long sessionId) {
        List<FutureTask<Void>> taskList = queuedTasksMap.get(sessionId);

        for(FutureTask<Void> fT : taskList) {
            fT.cancel(true);
        }

        queuedTasksMap.remove(sessionId);
    }

    public void cancelAllTasks() {
        for(Map.Entry<Long, List<FutureTask<Void>>> taskEntry : queuedTasksMap.entrySet()) {
            for(FutureTask<Void> fT : taskEntry.getValue()) {
                fT.cancel(true);
            }
        }
    }

    private void submitTask(long sessionId, Runnable task) {
        FutureTask<Void> futureTask = new FutureTask<>(task, null) {
            /**
             * Override the done method to remove itself from the list once completed
             */
            @Override
            protected void done() {
                List<FutureTask<Void>> taskList = queuedTasksMap.get(sessionId);
                if(taskList != null) {
                    taskList.remove(this);
                }
            }
        };
        executorService.submit(futureTask);

        List<FutureTask<Void>> sessionTasks = new ArrayList<>();
        if(queuedTasksMap.containsKey(sessionId)) {
            sessionTasks = queuedTasksMap.get(sessionId);
        }
        else {
            queuedTasksMap.put(sessionId, sessionTasks);
        }
        sessionTasks.add(futureTask);
    }

    private String[] getRecommendationsFromSpotify(RecommenderSession session) {
        ArrayList<String> songsFromYoutube = determineSongsFromYoutube(session);
        return recommenderRequester.getSongsFromSpotify(songsFromYoutube).getRecommendedSongs();
    }

    private ArrayList<String> determineSongsFromYoutube(RecommenderSession session) {
        ArrayList<String> trackNames = new ArrayList<>();

        for(Object song : session.getSearchedSongs()) {
            String songStr = song.toString();
            if(songStr.startsWith("https://www.youtube") || songStr.startsWith("https://youtu.be") || songStr.startsWith("https://youtube")) {
                // If it's a YouTube URL, check if it's already in the queue
                for(AudioTrack track : session.getAudioQueue()) {
                    if(track.getInfo().uri.equals(songStr)) {
                        // ensure it will not exceed 100 character limit
                        trackNames.add(track.getInfo().title.length() < 100 ?
                                track.getInfo().title : track.getInfo().title.substring(0, 100));
                        break;
                    }
                }
            } else {
                // If it's not a YouTube URL, simply add it to trackNames
                // ensure it will not exceed 100 character limit
                trackNames.add(songStr.length() < 100 ?
                        songStr : songStr.substring(0, 100));
            }
        }
        return trackNames;
    }

}
