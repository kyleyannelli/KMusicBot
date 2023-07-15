package SongRecommender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.javacord.api.DiscordApi;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import SpotifyApi.RadioSongsHandler;
import se.michaelthelin.spotify.SpotifyApi;

public class RecommenderProcessor {

    private final ExecutorService executorService;

    // Spotify and Discord API objects are persistent the entire application life time
    private final DiscordApi discordApi;
    private final SpotifyApi spotifyApi;

    private final RecommenderRequester recommenderRequester;

    public RecommenderProcessor(DiscordApi discordApi, SpotifyApi spotifyApi, int maxThreads) {
        this.executorService = Executors.newFixedThreadPool(maxThreads);

        this.discordApi = discordApi;
        this.spotifyApi = spotifyApi;

        this.recommenderRequester = new RecommenderRequester();
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

    public ArrayList<String> determineSongsFromYoutube(RecommenderSession session) {
        ArrayList<String> trackNames = new ArrayList<>();

        for(Object song : session.getSearchedSongs()) {
            String songStr = song.toString();
            if(songStr.startsWith("https://www.youtube") || songStr.startsWith("https://youtu.be") || songStr.startsWith("https://youtube")) {
                // If it's a YouTube URL, check if it's already in the queue
                for(AudioTrack track : session.getAudioQueue()) {
                    if(track.getInfo().uri.equals(songStr)) {
                        trackNames.add(track.getInfo().title);
                        break;
                    }
                }
            } else {
                // If it's not a YouTube URL, simply add it to trackNames
                trackNames.add(songStr);
            }
        }
        return trackNames;
    }

    public String[] getRecommendationsFromSpotify(List<String> songs) { 
        try {
            String[] recommendedTracks = RadioSongsHandler.generateRecommendationsFromList(spotifyApi, songs);

            if(recommendedTracks == null || recommendedTracks.length == 0) return new String[0];

            return recommendedTracks;
        }
        catch(Exception e) {
            // log reason
            return new String[0];
        }
    }
}
