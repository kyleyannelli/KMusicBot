package dev.kmfg.discordbot.commands;

import dev.kmfg.helpers.EnsuredSlashCommandInteraction;
import dev.kmfg.helpers.QueueResult;
import dev.kmfg.sessions.SessionManager;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Handles the /search command by loading in results from SearchResultAudioHandler and passing it to the messageSender object.
 */
public class SearchCommand extends Command {

    public SearchCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
        super(sessionManager, slashCommandEvent, respondLater);
    }

    @Override
    public void execute() {
        // create param requirement for song
        ArrayList<String> params = new ArrayList<>(List.of("song"));
        EnsuredSlashCommandInteraction ensuredInteraction = this.getEnsuredInteraction(params);
        // in the case that it is null, early return. ensured interaction handles the messaging
        if(ensuredInteraction == null) return;

        String searchQuery = ensuredInteraction.getParameterValue("song");
        QueueResult searchResults = ensuredInteraction.getAudioSession().getLavaSource().getListQueryResults(searchQuery);

        // if no tracks were found
        if(searchResults.getQueuedTracks().isEmpty()) {
            this.messageSender.sendNothingFoundEmbed(searchQuery);
            return;
        }

        this.messageSender.sendSearchResultEmbed(
                searchResults.getQueuedTracks(),
                ensuredInteraction.getAudioSession().getAssociatedServerId()
        );
    }
}
