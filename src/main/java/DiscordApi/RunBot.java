package DiscordApi;

import Lavaplayer.LavaplayerAudioSource;
import SongRecommender.RecommenderProcessor;
import SpotifyApi.ClientCreate;
import io.github.cdimascio.dotenv.Dotenv;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import se.michaelthelin.spotify.SpotifyApi;

public class RunBot {
    public static SpotifyApi spotifyApi;
    public static void main(String[] args) {
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
        spotifyApi = ClientCreate.clientCredentials_Sync();
        RecommenderProcessor recommenderProcessor = new RecommenderProcessor(api, spotifyApi, 10);
        // listen for all commands
        KCommands.listenForAllCommands(api);
        KCommands.addRecommenderProcessor(recommenderProcessor);
        addListeners(api);
    }
    
    public static void addListeners(DiscordApi api) {
        api.addServerJoinListener(event -> KCommands.isEphemeral.put(event.getServer().getId(), false));
        api.addServerJoinListener(event -> MySQL.SetupDatabase.setup(event.getServer().getIdAsString()));
        api.addServerLeaveListener(event -> {
            KCommands.isEphemeral.remove(event.getServer().getId());
            if(LavaplayerAudioSource.getPlayerByServerId(event.getServer().getId()) != null) {
                LavaplayerAudioSource.getPlayerByServerId(event.getServer().getId()).destroy();
                LavaplayerAudioSource.removePlayerByServerId(event.getServer().getId());
            }
        });
    }
}
