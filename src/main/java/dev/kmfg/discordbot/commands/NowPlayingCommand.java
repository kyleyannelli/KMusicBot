package dev.kmfg.discordbot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.kmfg.helpers.EnsuredSlashCommandInteraction;
import dev.kmfg.sessions.SessionManager;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

import java.util.concurrent.CompletableFuture;

/**
 * Handles the /np command by sending out an EmbedMessage of the current playing track
 */
public class NowPlayingCommand extends Command {
    public NowPlayingCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
        super(sessionManager, slashCommandEvent, respondLater);
    }

    // javadoc is still exactly applicable from super
    @Override
    public void execute() {
        // no parameters so pass through null
        EnsuredSlashCommandInteraction ensuredInteraction = this.getEnsuredInteraction(null);
        // make sure the ensured interaction is not null, otherwise early return. getEnsuredInteraction will handle the messages.
        if(ensuredInteraction == null) return;

        // get the current track
        AudioTrack currentAudioTrack = ensuredInteraction.getAudioSession()
                .getLavaSource()
                .getCurrentPlayingAudioTrack();
        // current track can be null, so make sure it isn't before continuing.
        if(currentAudioTrack == null) {
            this.messageSender.sendNothingPlayingEmbed();
            // early return
            return;
        }

        // send the now playing embed
        this.messageSender.sendNowPlayingEmbed(currentAudioTrack);
    }
}
