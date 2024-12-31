package dev.kmfg.musicbot.core.commands.executors;

import dev.kmfg.musicbot.core.util.slashcommands.EnsuredSlashCommandInteraction;
import dev.kmfg.musicbot.core.lavaplayer.PositionalAudioTrack;
import dev.kmfg.musicbot.core.sessions.SessionManager;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

public class ViewQueueCommand extends Command {
    public static final String COMMAND_NAME = "queue";
    private static final String DESCRIPTION = "View the queue of tracks at a specified page number.";
    private static final int PAGE_MAX_ROWS = 5;

    public ViewQueueCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandCreateEvent,
            ExecutorService executorService) {
        super(sessionManager, slashCommandCreateEvent, executorService);
    }

    public ViewQueueCommand() {
        super();
    }

    @Override
    public void register(DiscordApi discordApi) {
        SlashCommand.with(COMMAND_NAME, DESCRIPTION,
                Collections.singletonList(
                        SlashCommandOption
                                .create(SlashCommandOptionType.LONG,
                                        "pageNumber",
                                        "The page of queue to view.",
                                        true)))
                .createGlobal(discordApi);
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
        ArrayList<String> params = new ArrayList<>();
        params.add("pagenumber");

        EnsuredSlashCommandInteraction ensuredSlashCommandInteraction = getEnsuredInteraction(params);
        if (ensuredSlashCommandInteraction == null)
            return;

        int requestedPageNumber = parsePageNumber(ensuredSlashCommandInteraction.getParameterValue("pagenumber"));

        if (requestedPageNumber <= 0) {
            this.messageSender.sendBadParameterEmbed("pagenumber");
            return;
        }

        ArrayList<PositionalAudioTrack> audioTrackQueue = ensuredSlashCommandInteraction.getAudioSession()
                .getPositionalAudioQueue();

        int totalPages;

        if (audioTrackQueue == null || audioTrackQueue.size() == 0) {
            this.messageSender.sendEmptyQueueEmbed();
            return;
        }

        totalPages = calculateTotalPages(audioTrackQueue.size());

        if (totalPages < requestedPageNumber) {
            this.messageSender.sendOutOfBoundsEmbed(requestedPageNumber, totalPages);
            return;
        }

        ArrayList<PositionalAudioTrack> relevantAudioTracks = getRelevantAudioTracks(audioTrackQueue,
                requestedPageNumber);
        this.messageSender.sendViewQueueEmbed(relevantAudioTracks, requestedPageNumber, totalPages);
    }

    private ArrayList<PositionalAudioTrack> getRelevantAudioTracks(ArrayList<PositionalAudioTrack> allTracks,
            int pageNumber) {
        ArrayList<PositionalAudioTrack> relevantTracks = new ArrayList<>();

        int startIndex = ((pageNumber - 1) * PAGE_MAX_ROWS);
        int endIndex = Math.min(startIndex + PAGE_MAX_ROWS, allTracks.size());

        for (int i = startIndex; i < endIndex; i++) {
            relevantTracks.add(allTracks.get(i));
        }

        return relevantTracks;
    }

    private int parsePageNumber(String value) {
        int pageNumber;

        try {
            pageNumber = Integer.parseInt(value);
        } catch (NumberFormatException numberFormatException) {
            Logger.error("Failed to parse value \"" + value + "\" for page number.");
            pageNumber = -1;
        }
        return pageNumber;
    }

    private int calculateTotalPages(int totalItems) {
        return (int) Math.ceil((double) totalItems / PAGE_MAX_ROWS);
    }
}
