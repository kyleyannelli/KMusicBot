package dev.kmfg.musicbot.core.commands.executors;

import dev.kmfg.musicbot.core.sessions.SessionManager;
import dev.kmfg.musicbot.core.util.slashcommands.EnsuredSlashCommandInteraction;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.tinylog.Logger;

import java.util.Optional;

public class StopCommand extends Command {
    public static final String COMMAND_NAME = "stop";
    public static final String DESCRIPTION = "Stop the music, including the queue, then disconnect the bot.";
    public StopCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent) {
        super(sessionManager, slashCommandEvent);
    }

    public StopCommand() {
        super();
    }

    @Override
    public void register(DiscordApi discordApi) {
        SlashCommand.with(COMMAND_NAME, DESCRIPTION).createGlobal(discordApi);
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public String getCommandDescription() {
        return DESCRIPTION;
    }

    @Override
    public void execute() {
        Optional<Server> serverOptional = this.slashCommandEvent.getSlashCommandInteraction().getServer();

        if(serverOptional.isPresent() && isInVoiceChannel(serverOptional.get())) {
            EnsuredSlashCommandInteraction ensuredInteraction = getEnsuredInteraction(null);
            ensuredInteraction.getAudioSession().stopAllTracks();
        }
        else {
            this.messageSender.sendNothingPlayingEmbed();
            return;
        }
    }

    public boolean isInVoiceChannel(Server server) {
        return server.getAudioConnection().isPresent();
    }
}
