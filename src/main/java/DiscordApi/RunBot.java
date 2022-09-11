package DiscordApi;

import io.github.cdimascio.dotenv.Dotenv;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

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
        // create SlashCommand /play
        KCommands.listenForAllCommands(api);
    }
}
