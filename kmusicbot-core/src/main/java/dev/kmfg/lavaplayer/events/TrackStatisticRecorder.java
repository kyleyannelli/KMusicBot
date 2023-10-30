package dev.kmfg.lavaplayer.events;

import java.util.Optional;

import org.hibernate.SessionFactory;
import org.tinylog.Logger;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import dev.kmfg.database.models.DiscordGuild;
import dev.kmfg.database.models.KMusicSong;
import dev.kmfg.database.models.SongInitialization;
import dev.kmfg.database.models.TrackedSong;
import dev.kmfg.database.repositories.DiscordGuildRepo;
import dev.kmfg.database.repositories.KMusicSongRepo;
import dev.kmfg.database.repositories.SongInitializationRepo;
import dev.kmfg.database.repositories.TrackedSongRepo;
import dev.kmfg.sessions.AudioSession;

public class TrackStatisticRecorder implements TrackEventListener {
    private final SessionFactory sessionFactory;
    private final KMusicSongRepo kmusicSongRepo;
    private final TrackedSongRepo trackedSongRepo;
    private final DiscordGuildRepo discordGuildRepo;
    private final SongInitializationRepo songInitializationRepo;

    public TrackStatisticRecorder(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.kmusicSongRepo = new KMusicSongRepo(sessionFactory);
        this.trackedSongRepo = new TrackedSongRepo(sessionFactory);
        this.discordGuildRepo = new DiscordGuildRepo(sessionFactory);
        this.songInitializationRepo = new SongInitializationRepo(sessionFactory);
    }

    @Override
    public void onTrackEvent(TrackEvent trackEvent) {
        AudioSession audioSession = trackEvent.getAudioSession();
        AudioTrack audioTrack = trackEvent.getAudioTrack();
        if(trackEvent instanceof TrackStartEvent) {
        }
        else if(trackEvent instanceof TrackEndEvent) {

        }
        else {
            StringBuilder stringBuilder = new StringBuilder()
                .append(trackEvent.getClass().toString())
                .append(" was uncaught in TrackStatisticRecorder. It is likely missing from the onTrackEvent method!");
            Logger.warn(stringBuilder.toString());
        }
    }

    private void handleTrackStartEvent(TrackStartEvent trackStartEvent) {
        String youtubeUri = trackStartEvent.getAudioTrack().getInfo().uri;
        AudioSession audioSession = trackStartEvent.getAudioSession();
        KMusicSong kmusicSong = kmusicSongRepo.saveOrGet(new KMusicSong(youtubeUri));
        DiscordGuild discordGuild = discordGuildRepo.saveOrGet(new DiscordGuild(audioSession.getAssociatedServerId()));
        TrackedSong trackedSong = trackedSongRepo.saveOrGet(new TrackedSong(discordGuild, kmusicSong));
        // the updated_at column signifies the last time the track was played.
        trackedSong.refreshUpdatedAt();
        trackedSong = trackedSongRepo.save(trackedSong).get();
    }
}

