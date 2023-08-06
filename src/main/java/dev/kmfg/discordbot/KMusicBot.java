package dev.kmfg.discordbot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import dev.kmfg.discordbot.CommandsListener;
import dev.kmfg.discordbot.listenerhandlers.UserLeaveVoiceListenerHandler;
import dev.kmfg.sessions.SessionManager;
import dev.kmfg.songrecommender.RecommenderProcessor;
import dev.kmfg.spotifyapi.ClientCreate;
import io.github.cdimascio.dotenv.Dotenv;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.tinylog.Logger;
import se.michaelthelin.spotify.SpotifyApi;

/**
 * KMusicBot fully allows for a music bot to operate. After initializing, calling start() on the object will launch the bot.
 */
public class KMusicBot {
    private static final Dotenv dotenv = Dotenv.load();
    // if the .env file does not have MAX_RECOMMENDER_THREADS, use default value of 10, else, use the .env value
    protected static final int MAX_RECOMMENDER_THREADS = dotenv.get("MAX_RECOMMENDER_THREADS") == null ?
            10 : Integer.parseInt(dotenv.get("MAX_RECOMMENDER_THREADS"));;
    protected DiscordApiBuilder discordApiBuilder;
    protected DiscordApi discordApi;
    protected SessionManager sessionManager;
    protected CommandsListener commandsListener;

    public KMusicBot() {
        // pull variables from .env file
        Dotenv dotenv = Dotenv.load();
        this.discordApiBuilder = new DiscordApiBuilder()
                // set the token (from .env)
                .setToken(dotenv.get("DISCORD_BOT_TOKEN"))
                // set all intents possible that don't require privileges
                .setAllNonPrivilegedIntents();
    }

    /**
     * Makes the bot login to Discord.
     * Additionally, setting up the SessionManager and CommandListener.
     */
    public void start() {
        // login to discord (sync)
        this.discordApi = this.discordApiBuilder.login().join();
        this.discordApiBuilder = null;
        Logger.info("Bot logged into discord.");

        // setup session manager, this must be done before setting up the CommandsListener
        this.setupSessionManager();
        Logger.info("SessionManager setup.");
        // now we can listen for the commands
        this.setupCommandsListener();
        Logger.info("CommandsListener setup and listening.");

        // Create all listeners
        this.setupListeners();
    }

    /**
     * Setup all the listeners.
     * This is just a wrapper for every listener so an inner function is more clean.
     */
    protected void setupListeners() {
        this.listenForServerVoiceChannelLeaves();
    }

    /**
     * Listen for ServerVoiceChannel leaves and pass the handling to UserLeaveVoiceListenerHandler
     */
    protected void listenForServerVoiceChannelLeaves() {
        // UserLeaveVoiceListenerHandler will handle the event
        this.discordApi.addServerVoiceChannelMemberLeaveListener(new UserLeaveVoiceListenerHandler(this.sessionManager));
    }

    /**
     * This MUST be called after the discordApi has been properly initialized.
     * Sets up the SessionManager.
     * Registers YoutubeAudio as a SourceManager.
     * Creates the SpotifyApi object used by the SessionManager and RecommenderProcessor.
     * This also creates the RecommenderProcessor.
     */
    protected void setupSessionManager() {
        // Create a player manager
        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        // set Youtube as the audio source
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        // Create the spotify API object. This is used by the RecommenderProcessor
        SpotifyApi spotifyApi = ClientCreate.clientCredentials_Sync();
        // create the recommender processor for RecommenderSessions (and its child classes)
        RecommenderProcessor recommenderProcessor = new RecommenderProcessor(this.discordApi, spotifyApi, MAX_RECOMMENDER_THREADS);
        // create session manager for AudioSessions
        this.sessionManager = new SessionManager(this.discordApi, spotifyApi, playerManager, recommenderProcessor);
    }

    /**
     * This MUST be called after the discordApi and sessionManager objects are properly initialized.
     * Initializes the CommandsListener with the SessionManager, then sets it to listen for the commands.
     */
    protected void setupCommandsListener() {
        this.commandsListener = new CommandsListener(this.discordApi, sessionManager);
        this.commandsListener.createAndListenForGlobalCommands();
    }
}