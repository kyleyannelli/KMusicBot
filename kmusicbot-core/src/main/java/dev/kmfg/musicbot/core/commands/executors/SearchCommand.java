package dev.kmfg.musicbot.core.commands.executors;

import dev.kmfg.musicbot.core.util.slashcommands.EnsuredSlashCommandInteraction;
import dev.kmfg.musicbot.core.sessions.SessionManager;
import dev.kmfg.musicbot.core.util.sessions.QueueResult;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles the /search command by loading in results from SearchResultAudioHandler and passing it to the messageSender object.
 */
public class SearchCommand extends Command {
    public static final String COMMAND_NAME = "search";
    private static final String DESCRIPTION = "Search for a song";
    public SearchCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent) {
        super(sessionManager, slashCommandEvent);
    }

    public SearchCommand() {
        super();
    }

    @Override
    public void register(DiscordApi discordApi) {
        SlashCommand.with(COMMAND_NAME, DESCRIPTION,
                        // create option(s)
                        Collections.singletonList(
                                // create option /search <song>
                                SlashCommandOption.create(SlashCommandOptionType.STRING, "song", "The song to search for", true)
                        ))
                .createGlobal(discordApi).join();
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
        // create param requirement for song
        ArrayList<String> params = new ArrayList<>(List.of("song"));
        EnsuredSlashCommandInteraction ensuredInteraction = this.getEnsuredInteraction(params);
        // in the case that it is null, early return. ensured interaction handles the messaging
        if(ensuredInteraction == null) return;

        String searchQuery = ensuredInteraction.getParameterValue("song");
        QueueResult searchResults = ensuredInteraction.getAudioSession().getLavaSource().getListQueryResults(this.discordUser, searchQuery);

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
