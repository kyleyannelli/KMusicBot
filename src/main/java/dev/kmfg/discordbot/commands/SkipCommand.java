package dev.kmfg.discordbot.commands;

import dev.kmfg.sessions.SessionManager;
import dev.kmfg.helpers.EnsuredSlashCommandInteraction;
import dev.kmfg.helpers.QueueResult;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

import java.util.concurrent.CompletableFuture;

public class SkipCommand extends Command {
    public SkipCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
        super(sessionManager, slashCommandEvent, respondLater);
    }
    @Override
    public void execute() {
        // begin ensured interaction setup
        // we can pass through null if there are no params
        EnsuredSlashCommandInteraction ensuredInteraction = getEnsuredInteraction(null);
        // Above method will handle sending messages, stop execution here if we don't get an EnsuredInteraction.
        if(ensuredInteraction == null) return;

        // skipCurrentPlaying skips current into [0], and new goes into [1]
        QueueResult[] queueResults = ensuredInteraction.getAudioSession().skipCurrentPlaying();

        // if [1] is success, that means a new track is going to play
        if(queueResults[1].isSuccess()) {
            this.messageSender.setForcedTitle("Now Playing");
            this.messageSender.sendQueueResultEmbed(queueResults[1]);
        }
        // else if a track was skipped
        else if(queueResults[0].isSuccess()) {
            this.messageSender.setForcedTitle("Stopped.");
            this.messageSender.sendQueueResultEmbed(queueResults[0]);
        }
        // else nothing was playing and nothing was skipped!
        else {
            this.messageSender.sendNothingPlayingEmbed();
        }
    }
}
