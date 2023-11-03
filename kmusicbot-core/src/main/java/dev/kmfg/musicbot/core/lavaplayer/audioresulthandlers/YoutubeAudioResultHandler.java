package dev.kmfg.musicbot.core.lavaplayer.audioresulthandlers;

import dev.kmfg.musicbot.database.models.DiscordUser;
import dev.kmfg.musicbot.core.util.sessions.SingleUse;
import dev.kmfg.musicbot.core.lavaplayer.AudioTrackWithUser;
import dev.kmfg.musicbot.core.lavaplayer.ProperTrackScheduler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.tinylog.Logger;

public class YoutubeAudioResultHandler extends KAudioResultHandler {
    public YoutubeAudioResultHandler(ProperTrackScheduler trackScheduler, DiscordUser discordUser) {
        super(trackScheduler, discordUser);
    }

    @Override
    public void loadFailed(FriendlyException arg0) {
        Logger.error(arg0, "Track failed to load!");
        this.isSuccess = new SingleUse<>(false);
    }

    @Override
    public void noMatches() {
        Logger.warn("No matches found for query!");
        this.isSuccess = new SingleUse<>(false);
    }

    @Override
    public void playlistLoaded(AudioPlaylist arg0) {
        ArrayList<AudioTrackWithUser> tracksWithUser = arg0.getTracks().stream()
                .map(track -> new AudioTrackWithUser(track, discordUser))
                .collect(Collectors.toCollection(ArrayList::new));
        boolean loadPlaylistResult = trackScheduler.loadPlaylist(tracksWithUser, this.deprioritizeQueue, this.playNext);
        // log result
        if(loadPlaylistResult) {
            Logger.info("Playlist successfully loaded.");
            //ArrayList<AudioTrack> loadedTracks = new ArrayList(arg0.getTracks());
            ArrayList<AudioTrackWithUser> loadedTracks = new ArrayList<>();
            arg0.getTracks().stream().forEach(track -> loadedTracks.add(new AudioTrackWithUser(track, this.discordUser)));
            this.lastLoadedTracks = new SingleUse<>(loadedTracks);
        }
        else Logger.warn("Playlist failed to load!");

        this.isSuccess = new SingleUse<>(loadPlaylistResult);
    }

    @Override
    public void trackLoaded(AudioTrack arg0) {
        AudioTrackWithUser audioTrackWithUser = new AudioTrackWithUser(arg0, discordUser);
        if(this.playNext && this.trackScheduler.hasNowPlaying()) {
            trackScheduler.queueNext(audioTrackWithUser);
        }
        else {
            trackScheduler.loadSingleTrack(audioTrackWithUser, this.deprioritizeQueue);
        }
        this.lastLoadedTracks = new SingleUse<>(new ArrayList<>(List.of(audioTrackWithUser)));
        this.isSuccess = new SingleUse<>(true);
    }
}
