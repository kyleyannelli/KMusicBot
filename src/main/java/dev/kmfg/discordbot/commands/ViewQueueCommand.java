package dev.kmfg.discordbot.commands;

import dev.kmfg.sessions.SessionManager;
import dev.kmfg.helpers.EnsuredSlashCommandInteraction;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class ViewQueueCommand extends Command {
    private static final int PAGE_MAX_ROWS = 5;

    public ViewQueueCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandCreateEvent, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
        super(sessionManager, slashCommandCreateEvent, respondLater);
    }

    @Override
    public void execute() {
        ArrayList<String> params = new ArrayList<>();
        params.add("pagenumber");

        EnsuredSlashCommandInteraction ensuredSlashCommandInteraction = getEnsuredInteraction(params);
        if(ensuredSlashCommandInteraction == null) return;

        int requestedPageNumber = parsePageNumber(ensuredSlashCommandInteraction.getParameterValue("pagenumber"));

        if(requestedPageNumber <= 0) {
            this.messageSender.sendBadParameterEmbed("pagenumber");
            return;
        }

        ArrayList<AudioTrack> audioTrackQueue = ensuredSlashCommandInteraction.getAudioSession().getNullSeparatedAudioQueue();

        int totalPages;

        if(audioTrackQueue == null || audioTrackQueue.size() - 1 == 0) {
            this.messageSender.sendEmptyQueueEmbed();
            return;
        }

        totalPages = calculateTotalPages(audioTrackQueue.size() - 1);

        if(totalPages < requestedPageNumber) {
            this.messageSender.sendOutOfBoundsEmbed(requestedPageNumber, totalPages);
            return;
        }

        ArrayList<AudioTrack> relevantAudioTracks = getRelevantAudioTracks(audioTrackQueue, requestedPageNumber);
        this.messageSender.sendViewQueueEmbed(relevantAudioTracks, requestedPageNumber, totalPages);
    }

    private ArrayList<AudioTrack> getRelevantAudioTracks(ArrayList<AudioTrack> allTracks, int pageNumber) {
        ArrayList<AudioTrack> relevantTracks = new ArrayList<>();

        int startIndex = ((pageNumber - 1) * PAGE_MAX_ROWS);
        int endIndex = Math.min(startIndex + PAGE_MAX_ROWS, allTracks.size());

        // handle the case that we are starting at an autoqueue position
        if(startIndex < 0 && allTracks.get(startIndex - 1) == null) {
            relevantTracks.add(null);
        }

        for(int i = startIndex; i < endIndex; i++) {
            if(allTracks.get(i) == null && i < 0) {
                i--;
            }
            relevantTracks.add(allTracks.get(i));
        }

        return relevantTracks;
    }

    private int parsePageNumber(String value) {
        int pageNumber;

        try {
            pageNumber = Integer.parseInt(value);
        }
        catch(NumberFormatException numberFormatException) {
            Logger.error("Failed to parse value \"" + value + "\" for page number.");
            pageNumber = -1;
        }
        return pageNumber;
    }

    private int calculateTotalPages(int totalItems) {
        return (int) Math.ceil((double) totalItems / PAGE_MAX_ROWS);
    }
}
