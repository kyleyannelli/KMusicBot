package dev.kmfg.lavaplayer.events;

import org.hibernate.SessionFactory;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.user.User;
import org.tinylog.Logger;

import dev.kmfg.database.models.DiscordGuild;
import dev.kmfg.database.models.DiscordUser;
import dev.kmfg.database.models.KMusicSong;
import dev.kmfg.database.models.SongInitialization;
import dev.kmfg.database.models.SongPlaytime;
import dev.kmfg.database.models.TrackedSong;
import dev.kmfg.database.repositories.DiscordGuildRepo;
import dev.kmfg.database.repositories.DiscordUserRepo;
import dev.kmfg.database.repositories.KMusicSongRepo;
import dev.kmfg.database.repositories.SongInitializationRepo;
import dev.kmfg.database.repositories.SongPlaytimeRepo;
import dev.kmfg.database.repositories.TrackedSongRepo;
import dev.kmfg.lavaplayer.AudioTrackWithUser;
import dev.kmfg.sessions.AudioSession;

public class TrackStatisticRecorder implements TrackEventListener {
    private final KMusicSongRepo kmusicSongRepo;
    private final TrackedSongRepo trackedSongRepo;
    private final DiscordGuildRepo discordGuildRepo;
    private final SongInitializationRepo songInitializationRepo;
    private final SongPlaytimeRepo songPlaytimeRepo;
    private final DiscordUserRepo discordUserRepo;

    public TrackStatisticRecorder(SessionFactory sessionFactory) {
        this.kmusicSongRepo = new KMusicSongRepo(sessionFactory);
        this.trackedSongRepo = new TrackedSongRepo(sessionFactory);
        this.discordGuildRepo = new DiscordGuildRepo(sessionFactory);
        this.songInitializationRepo = new SongInitializationRepo(sessionFactory);
        this.discordUserRepo = new DiscordUserRepo(sessionFactory);
        this.songPlaytimeRepo = new SongPlaytimeRepo(sessionFactory);
    }

    @Override
    public void onTrackEvent(TrackEvent trackEvent) {
        if(trackEvent instanceof TrackStartEvent) {
            this.handleTrackStartEvent((TrackStartEvent) trackEvent);
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
        TrackedSong trackedSong = this.setupGeneralModels(trackStartEvent);
        this.trackIndividualUsersStart(trackStartEvent.getAudioSession(), trackedSong);
    }

    protected void trackIndividualUsersStart(AudioSession audioSession, TrackedSong trackedSong) {
        DiscordGuild discordGuild = trackedSong.getGuild();
        ServerVoiceChannel serverVoiceChannel = audioSession.getDiscordApi()
            .getServerById(discordGuild.getDiscordId()).get()
            .getConnectedVoiceChannel(audioSession.getDiscordApi().getYourself())
            .get();

        for(User connectedUser : serverVoiceChannel.getConnectedUsers()) {
            trackUserStart(connectedUser, trackedSong);
        }
    }

    protected void trackUserStart(User connectedUser, TrackedSong trackedSong) {
        DiscordUser discordUser = this.discordUserRepo.saveOrGet(new DiscordUser(connectedUser.getId(), connectedUser.getDiscriminatedName()));
        SongPlaytime songPlaytime = this.songPlaytimeRepo.saveOrGet(new SongPlaytime(discordUser, trackedSong));
        songPlaytime.refreshUpdatedAt();
        songPlaytimeRepo.save(songPlaytime);
    }

    protected TrackedSong setupGeneralModels(TrackStartEvent trackStartEvent) {
        AudioTrackWithUser audioTrackWithUser = trackStartEvent.getAudioTrackWithUser();
        AudioSession audioSession = trackStartEvent.getAudioSession();
        DiscordUser discordUser = audioTrackWithUser.getDiscordUser();
        String youtubeUri = audioTrackWithUser.getAudioTrack().getInfo().uri;
        // generate or get the general song, as of now just a youtubeUri that can be used across different servers.
        KMusicSong kmusicSong = kmusicSongRepo.saveOrGet(new KMusicSong(youtubeUri));
        // generate or get the server (guild)
        DiscordGuild discordGuild = discordGuildRepo.saveOrGet(new DiscordGuild(audioSession.getAssociatedServerId()));
        // generate or get the discord user
        discordUser = discordUserRepo.saveOrGet(discordUser);
        // generate or get TrackedSong, this is the general tracking for the server.
        TrackedSong trackedSong = trackedSongRepo.saveOrGet(new TrackedSong(discordGuild, kmusicSong));
        trackedSong.refreshUpdatedAt(); // the updated_at column signifies the last time the track was played.
        trackedSong = trackedSongRepo.save(trackedSong).get();
        // generate or get the initialization
        songInitializationRepo.saveOrGet(new SongInitialization(trackedSong, discordUser));
        return trackedSong;
    }
}

