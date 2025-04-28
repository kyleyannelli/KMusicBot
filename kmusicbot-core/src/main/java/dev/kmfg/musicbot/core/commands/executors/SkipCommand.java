package dev.kmfg.musicbot.core.commands.executors;

import dev.kmfg.musicbot.core.util.slashcommands.EnsuredSlashCommandInteraction;
import dev.kmfg.musicbot.core.sessions.SessionManager;
import dev.kmfg.musicbot.core.util.sessions.QueueResult;

import java.util.concurrent.ExecutorService;

import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;

public class SkipCommand extends Command {
    public static final String COMMAND_NAME = "skip";
    private static final String DESCRIPTION = "Skip the current playing song.";

    public SkipCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent,
            ExecutorService executorService) {
        super(sessionManager, slashCommandEvent, executorService);
    }

    public SkipCommand() {
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
        super.execute();
        // begin ensured interaction setup
        // we can pass through null if there are no params
        EnsuredSlashCommandInteraction ensuredInteraction = getEnsuredInteraction(null);
        // Above method will handle sending messages, stop execution here if we don't
        // get an EnsuredInteraction.
        if (ensuredInteraction == null)
            return;

        // skipCurrentPlaying skips current into [0], and new goes into [1]
        QueueResult[] queueResults = ensuredInteraction.getAudioSession().skipCurrentPlaying();

        // if [1] is success, that means a new track is going to play
        if (queueResults[1].isSuccess()) {
            this.messageSender.sendQueueResultEmbed(queueResults[1], false);
        }
        // else if a track was skipped
        else if (queueResults[0].isSuccess()) {
            this.messageSender.setForcedTitle("Stopped.");
            this.messageSender.sendQueueResultEmbed(queueResults[0], false);
        }
        // else nothing was playing and nothing was skipped!
        else {
            this.messageSender.sendNothingPlayingEmbed();
        }
    }
}
