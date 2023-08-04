package dev.kmfg.discordbot;

import dev.kmfg.sessions.SessionManager;
import dev.kmfg.songrecommender.RecommenderProcessor;
import dev.kmfg.spotifyapi.ClientCreate;
import io.github.cdimascio.dotenv.Dotenv;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.tinylog.Logger;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;

import se.michaelthelin.spotify.SpotifyApi;

public class RunBot {
    private static final int MAX_RECOMMENDER_THREADS_DEFAULT = 10;

    public static void main(String[] args) {
        Logger.info("Bot Started!");
        // pull variables from .env file
        Dotenv dotenv = Dotenv.load();
        // connect to discord api with all non-privileged intents
        DiscordApi api = new DiscordApiBuilder()
                // set the token (from .env)
                .setToken(dotenv.get("DISCORD_BOT_TOKEN"))
                // set all intents possible that don't require privileges
                .setAllNonPrivilegedIntents()
                // start the bot :)
                .login().join();

        SpotifyApi spotifyApi = ClientCreate.clientCredentials_Sync();

        int maxRecommenderThreads = dotenv.get("MAX_RECOMMENDER_THREADS") == null ? 
            MAX_RECOMMENDER_THREADS_DEFAULT : Integer.parseInt(dotenv.get("MAX_RECOMMENDER_THREADS"));
        
        // Create a player manager
        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());

        // create the recommender processor for RecommenderSessions (and its child classes)
        RecommenderProcessor recommenderProcessor = new RecommenderProcessor(api, spotifyApi, maxRecommenderThreads);

        // create session manager for AudioSessions
        SessionManager sessionManager = new SessionManager(api, spotifyApi, playerManager, recommenderProcessor);

        CommandsListener commands = new CommandsListener(api, sessionManager);
        commands.createAndListenForGlobalCommands();

        // make sure to properly handle a shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            recommenderProcessor.cancelAllTasks();
            recommenderProcessor.shutdown();
        }));
    }
}
