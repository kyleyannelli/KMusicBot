package dev.kmfg.musicbot.core.sessions;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import dev.kmfg.musicbot.core.songrecommender.RecommenderThirdParty;
import dev.kmfg.musicbot.core.songrecommender.youtubeapi.RecommenderYoutubeScraper;
import org.javacord.api.DiscordApi;
import org.tinylog.Logger;

import org.hibernate.SessionFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

import dev.kmfg.musicbot.core.lavaplayer.LavaSource;
import dev.kmfg.musicbot.core.songrecommender.RecommenderProcessor;
import se.michaelthelin.spotify.SpotifyApi;

/**
 * Thread safe handling of creation and closing of {@link AudioSession}'s.
 */
public class SessionManager {
    private final ConcurrentHashMap<Long, AudioSession> audioSessions;
    private final RecommenderProcessor recommenderProcessor;
    private final DiscordApi discordApi;
    private final AudioPlayerManager audioPlayerManager;
    private final SessionFactory sessionFactory;

    public SessionManager(SessionFactory sessionFactory, DiscordApi discordApi, RecommenderThirdParty recommenderThirdParty,
            AudioPlayerManager audioPlayerManager, RecommenderProcessor recommenderProcessor) {
        this.audioSessions = new ConcurrentHashMap<>();
        this.recommenderProcessor = recommenderProcessor;
        this.audioPlayerManager = audioPlayerManager;
        this.sessionFactory = sessionFactory;
        this.discordApi = discordApi;
    }

    public AudioSession createAudioSession(long serverId) {
        AudioSession audioSession = new AudioSession(this.sessionFactory, this.discordApi, this.recommenderProcessor,
                serverId, this::handleAudioSessionShutdown);
        LavaSource lavaSource = new LavaSource(discordApi, audioPlayerManager, audioSession.getSessionId(),
                audioSession);
        audioSession.setLavaSource(lavaSource);
        this.audioSessions.put(serverId, audioSession);
        return audioSession;
    }

    public ConcurrentHashMap<Long, AudioSession> getAllAudioSessions() {
        return this.audioSessions;
    }

    public AudioSession getAudioSession(long serverId) {
        return this.audioSessions.get(serverId);
    }

    public DiscordApi getDiscordApi() {
        return this.discordApi;
    }

    public int getTotalSessionCount() {
        return this.audioSessions.size();
    }

    public SessionFactory getSessionFactory() {
        return this.sessionFactory;
    }

    /**
     * If there is only a single AudioSession, it will be returned. If there are
     */
    public Optional<AudioSession> getOnlyAudioSession() {
        // if there isn't just one element, we don't want to return anything
        if (this.audioSessions.size() != 1)
            return Optional.empty();

        return Optional.of(
                this.audioSessions.elements().nextElement());
    }

    private void handleAudioSessionShutdown(long associatedServerId) {
        // remove the audiosession upon shutdown
        AudioSession session = this.audioSessions.get(associatedServerId);
        if (session == null) {
            return;
        }
        long sessionId = session.getAssociatedServerId();
        this.audioSessions.remove(associatedServerId);
        Logger.info("Session {} removed from server {}.", sessionId, associatedServerId);
    }
}
