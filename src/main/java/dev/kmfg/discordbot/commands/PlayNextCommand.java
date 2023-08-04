package dev.kmfg.discordbot.commands;

import dev.kmfg.helpers.EnsuredSlashCommandInteraction;
import dev.kmfg.helpers.QueueResult;
import dev.kmfg.sessions.SessionManager;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class PlayNextCommand extends Command {
    public PlayNextCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
        super(sessionManager, slashCommandEvent, respondLater);
    }

    @Override
    public void execute() {
        // begin ensured interaction setup
        String songParameter = "song";
        ArrayList<String> requiredParameters = new ArrayList<>();
        requiredParameters.add(songParameter);

        EnsuredSlashCommandInteraction ensuredInteraction = getEnsuredInteraction(requiredParameters);
        // Above method will handle sending messages, stop execution here if we don't get an EnsuredInteraction.
        if(ensuredInteraction == null) return;

        QueueResult queueResult = ensuredInteraction
                .getAudioSession()
                .queueSearchQueryNext(ensuredInteraction.getParameterValue(songParameter));

        this.messageSender.sendQueueResultEmbed(queueResult);
    }
}
