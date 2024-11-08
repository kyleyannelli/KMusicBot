package dev.kmfg.musicbot.core.events;

import dev.kmfg.musicbot.core.sessions.AudioSession;
import io.github.cdimascio.dotenv.Dotenv;
import org.hibernate.SessionFactory;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.user.User;
import org.tinylog.Logger;

import dev.kmfg.musicbot.database.models.DiscordGuild;
import dev.kmfg.musicbot.database.models.DiscordUser;
import dev.kmfg.musicbot.database.models.KMusicSong;
import dev.kmfg.musicbot.database.models.SongInitialization;
import dev.kmfg.musicbot.database.models.SongPlaytime;
import dev.kmfg.musicbot.database.models.TrackedSong;
import dev.kmfg.musicbot.database.repositories.DiscordGuildRepo;
import dev.kmfg.musicbot.database.repositories.DiscordUserRepo;
import dev.kmfg.musicbot.database.repositories.KMusicSongRepo;
import dev.kmfg.musicbot.database.repositories.SongInitializationRepo;
import dev.kmfg.musicbot.database.repositories.SongPlaytimeRepo;
import dev.kmfg.musicbot.database.repositories.TrackedSongRepo;
import dev.kmfg.musicbot.core.lavaplayer.AudioTrackWithUser;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrackStatisticRecorder implements TrackEventListener {
    private final KMusicSongRepo kmusicSongRepo;
    private final TrackedSongRepo trackedSongRepo;
    private final DiscordGuildRepo discordGuildRepo;
    private final SongInitializationRepo songInitializationRepo;
    private final SongPlaytimeRepo songPlaytimeRepo;
    private final DiscordUserRepo discordUserRepo;
    private final ExecutorService executorService;
    private static final int DEFAULT_MAX_THREADS = 10;

    public TrackStatisticRecorder(SessionFactory sessionFactory) {
        this.kmusicSongRepo = new KMusicSongRepo(sessionFactory);
        this.trackedSongRepo = new TrackedSongRepo(sessionFactory);
        this.discordGuildRepo = new DiscordGuildRepo(sessionFactory);
        this.songInitializationRepo = new SongInitializationRepo(sessionFactory);
        this.discordUserRepo = new DiscordUserRepo(sessionFactory);
        this.songPlaytimeRepo = new SongPlaytimeRepo(sessionFactory);
        this.executorService = Executors.newFixedThreadPool(
                Dotenv.load().get("MAX_COMMAND_THREADS") == null ? DEFAULT_MAX_THREADS
                        : Integer.parseInt(Dotenv.load().get("MAX_COMMAND_THREADS")));
    }

    @Override
    public void onTrackEvent(TrackEvent trackEvent) {
        executorService.submit(() -> {
            handleEvents(trackEvent);
        });
    }

    private void handleEvents(TrackEvent trackEvent) {
        if (trackEvent instanceof TrackStartEvent) {
            this.handleTrackStartEvent((TrackStartEvent) trackEvent);
        } else if (trackEvent instanceof TrackEndEvent) {
            this.handleTrackEndEvent((TrackEndEvent) trackEvent);
        } else if (trackEvent instanceof TrackStartIndividualEvent) {
            this.handleTrackIndividualStartEvent((TrackStartIndividualEvent) trackEvent);
        } else if (trackEvent instanceof TrackEndIndividualEvent) {
            this.handleTrackIndividualEndEvent((TrackEndIndividualEvent) trackEvent);
        } else {
            StringBuilder stringBuilder = new StringBuilder()
                    .append(trackEvent.getClass().toString())
                    .append(" was uncaught in TrackStatisticRecorder. It is likely missing from the onTrackEvent method!");
            Logger.warn(stringBuilder.toString());
        }
    }

    protected void handleTrackIndividualEndEvent(TrackEndIndividualEvent endIndividualEvent) {
        TrackedSong trackedSong = this.individualSetupGeneralModels(endIndividualEvent);
        this.trackUserEnd(endIndividualEvent.getUser(), trackedSong, Timestamp.from(Instant.now()));
    }

    protected void handleTrackIndividualStartEvent(TrackStartIndividualEvent startIndividualEvent) {
        TrackedSong trackedSong = this.individualSetupGeneralModels(startIndividualEvent);
        this.trackUserStart(startIndividualEvent.getUser(), trackedSong);
    }

    protected TrackedSong individualSetupGeneralModels(TrackEvent event) {
        AudioTrackWithUser audioTrackWithUser = event.getAudioTrackWithUser();
        AudioSession audioSession = event.getAudioSession();
        DiscordUser discordUser = audioTrackWithUser.getDiscordUser();
        String youtubeUri = audioTrackWithUser.getAudioTrack().getInfo().uri;
        String author = audioTrackWithUser.getAudioTrack().getInfo().author;
        String title = audioTrackWithUser.getAudioTrack().getInfo().title;
        // generate or get the general song, as of now just a youtubeUri that can be
        // used across different servers.
        // saving and getting because author and title was an added column in later
        // version. I want to ensure overwrites
        KMusicSong kmusicSong = kmusicSongRepo.save(new KMusicSong(youtubeUri, author, title)).get();
        // generate or get the server (guild)
        DiscordGuild discordGuild = discordGuildRepo.saveOrGet(new DiscordGuild(audioSession.getAssociatedServerId()));
        // generate or get the discord user
        discordUser = discordUserRepo.saveOrGet(discordUser);
        // generate or get TrackedSong, this is the general tracking for the server.
        TrackedSong trackedSong = trackedSongRepo.saveOrGet(new TrackedSong(discordGuild, kmusicSong));
        return trackedSong;
    }

    protected void handleTrackEndEvent(TrackEndEvent trackEndEvent) {
        TrackedSong trackedSong = this.endSetupGeneralModels(trackEndEvent);
        Timestamp now = Timestamp.from(Instant.now());
        int incBy = trackedSong.getUpdatedAt().compareTo(now) > 0 ? 0
                : (int) (now.getTime() - trackedSong.getUpdatedAt().getTime()) / 1000;
        trackedSong.incrementSecondsPlayed(incBy);
        trackedSong = trackedSongRepo.save(trackedSong).get();
        this.trackIndividualUsersEnd(trackEndEvent.getAudioSession(), trackedSong, now);
    }

    protected void trackIndividualUsersEnd(AudioSession audioSession, TrackedSong trackedSong, Timestamp now) {
        DiscordGuild discordGuild = trackedSong.getGuild();
        ServerVoiceChannel serverVoiceChannel = audioSession.getDiscordApi()
                .getServerById(discordGuild.getDiscordId()).get()
                .getConnectedVoiceChannel(audioSession.getDiscordApi().getYourself())
                .get();

        for (User connectedUser : serverVoiceChannel.getConnectedUsers()) {
            if (connectedUser.isYourself())
                continue;
            trackUserEnd(connectedUser, trackedSong, now);
        }
    }

    protected void trackUserEnd(User connectedUser, TrackedSong trackedSong, Timestamp now) {
        DiscordUser discordUser = this.discordUserRepo
                .saveOrGet(new DiscordUser(connectedUser.getId(), connectedUser.getDiscriminatedName()));
        SongPlaytime songPlaytime = this.songPlaytimeRepo.saveOrGet(new SongPlaytime(discordUser, trackedSong));
        int incBy = songPlaytime.getUpdatedAt().compareTo(now) > 0 ? 0
                : (int) (now.getTime() - songPlaytime.getUpdatedAt().getTime()) / 1000;
        songPlaytime.incSecondsListened(incBy);
        songPlaytimeRepo.save(songPlaytime);
    }

    protected void handleTrackStartEvent(TrackStartEvent trackStartEvent) {
        TrackedSong trackedSong = this.startSetupGeneralModels(trackStartEvent);
        this.trackIndividualUsersStart(trackStartEvent.getAudioSession(), trackedSong);
    }

    protected void trackIndividualUsersStart(AudioSession audioSession, TrackedSong trackedSong) {
        DiscordGuild discordGuild = trackedSong.getGuild();
        ServerVoiceChannel serverVoiceChannel = audioSession.getDiscordApi()
                .getServerById(discordGuild.getDiscordId()).get()
                .getConnectedVoiceChannel(audioSession.getDiscordApi().getYourself())
                .get();

        for (User connectedUser : serverVoiceChannel.getConnectedUsers()) {
            if (connectedUser.isYourself())
                continue;
            trackUserStart(connectedUser, trackedSong);
        }
    }

    protected void trackUserStart(User connectedUser, TrackedSong trackedSong) {
        DiscordUser discordUser = this.discordUserRepo
                .saveOrGet(new DiscordUser(connectedUser.getId(), connectedUser.getDiscriminatedName()));
        SongPlaytime songPlaytime = this.songPlaytimeRepo.saveOrGet(new SongPlaytime(discordUser, trackedSong));
        songPlaytime.refreshUpdatedAt();
        songPlaytimeRepo.save(songPlaytime);
    }

    protected TrackedSong endSetupGeneralModels(TrackEndEvent trackEndEvent) {
        AudioTrackWithUser audioTrackWithUser = trackEndEvent.getAudioTrackWithUser();
        AudioSession audioSession = trackEndEvent.getAudioSession();
        DiscordUser discordUser = audioTrackWithUser.getDiscordUser();
        String youtubeUri = audioTrackWithUser.getAudioTrack().getInfo().uri;
        String author = audioTrackWithUser.getAudioTrack().getInfo().author;
        String title = audioTrackWithUser.getAudioTrack().getInfo().title;
        // generate or get the general song, as of now just a youtubeUri that can be
        // used across different servers.
        // saving and getting because author and title was an added column in later
        // version. I want to ensure overwrites
        KMusicSong kmusicSong = kmusicSongRepo.save(new KMusicSong(youtubeUri, author, title)).get();
        // generate or get the server (guild)
        DiscordGuild discordGuild = discordGuildRepo.saveOrGet(new DiscordGuild(audioSession.getAssociatedServerId()));
        // generate or get the discord user
        discordUser = discordUserRepo.saveOrGet(discordUser);
        // generate or get TrackedSong, this is the general tracking for the server.
        TrackedSong trackedSong = trackedSongRepo.saveOrGet(new TrackedSong(discordGuild, kmusicSong));
        return trackedSong;
    }

    protected TrackedSong startSetupGeneralModels(TrackStartEvent trackStartEvent) {
        AudioTrackWithUser audioTrackWithUser = trackStartEvent.getAudioTrackWithUser();
        AudioSession audioSession = trackStartEvent.getAudioSession();
        DiscordUser discordUser = audioTrackWithUser.getDiscordUser();
        String youtubeUri = audioTrackWithUser.getAudioTrack().getInfo().uri;
        String author = audioTrackWithUser.getAudioTrack().getInfo().author;
        String title = audioTrackWithUser.getAudioTrack().getInfo().title;
        // generate or get the general song, as of now just a youtubeUri that can be
        // used across different servers.
        // saving and getting because author and title was an added column in later
        // version. I want to ensure overwrites
        KMusicSong kmusicSong = kmusicSongRepo.save(new KMusicSong(youtubeUri, author, title)).get();
        // generate or get the server (guild)
        DiscordGuild discordGuild = discordGuildRepo.saveOrGet(new DiscordGuild(audioSession.getAssociatedServerId()));
        // generate or get the discord user
        discordUser = discordUserRepo.saveOrGet(discordUser);
        // generate or get TrackedSong, this is the general tracking for the server.
        TrackedSong trackedSong = trackedSongRepo.saveOrGet(new TrackedSong(discordGuild, kmusicSong));
        trackedSong.refreshUpdatedAt(); // the updated_at column signifies the last time the track was played.
        trackedSong = trackedSongRepo.save(trackedSong).get();
        // generate or get the initialization
        SongInitialization songInitialization = songInitializationRepo
                .saveOrGet(new SongInitialization(trackedSong, discordUser));
        songInitialization.incTimesInitialized();
        songInitialization = songInitializationRepo.save(songInitialization).get();
        return trackedSong;
    }

    public void shutdown() {
        this.executorService.shutdown();
    }
}
