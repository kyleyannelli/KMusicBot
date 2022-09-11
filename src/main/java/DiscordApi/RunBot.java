package DiscordApi;

import Lavaplayer.LavaplayerAudioSource;
import io.github.cdimascio.dotenv.Dotenv;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.util.logging.FallbackLoggerConfiguration;

public class RunBot {
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
        // listen for all commands
        KCommands.listenForAllCommands(api);
        api.addServerJoinListener(event -> {
            KCommands.isEphemeral.put(event.getServer().getId(), false);
        });
        api.addServerLeaveListener(event -> {
            KCommands.isEphemeral.remove(event.getServer().getId());
            if(LavaplayerAudioSource.getPlayerByServerId(event.getServer().getId()) != null) {
                LavaplayerAudioSource.getPlayerByServerId(event.getServer().getId()).destroy();
                LavaplayerAudioSource.removePlayerByServerId(event.getServer().getId());
            }
        });
    }
}
