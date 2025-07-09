package dev.kmfg.musicbot.core.songrecommender;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;

public interface RecommenderThirdParty {
    /**
     * Recommends songs based on an input song.
     * @return Song titles
     */
    public String[] recommend(AudioTrack starterSong);

    /**
     * Recommends songs based on multiple input songs
     * @return Song titles
     */
    public String[] recommend(AudioTrack[] starterSongs);

    /**
     * Recommends songs based on multiple input songs
     * @return Song titles
     */
    public String[] recommend(List<AudioTrack> starterSongs);
}
