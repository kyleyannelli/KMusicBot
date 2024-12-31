package dev.kmfg.musicbot.core.commands.executors;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.kmfg.musicbot.core.util.slashcommands.EnsuredSlashCommandInteraction;
import dev.kmfg.musicbot.core.sessions.SessionManager;

import java.util.concurrent.ExecutorService;

import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;

/**
 * Handles the /np command by sending out an EmbedMessage of the current playing
 * track
 */
public class NowPlayingCommand extends Command {
    public static final String COMMAND_NAME = "np";
    private static final String DESCRIPTION = "Display the current playing song.";

    public NowPlayingCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent,
            ExecutorService executorService) {
        super(sessionManager, slashCommandEvent, executorService);
    }

    public NowPlayingCommand() {
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

    // javadoc is still exactly applicable from super
    @Override
    public void execute() {
        super.execute();
        // no parameters so pass through null
        EnsuredSlashCommandInteraction ensuredInteraction = this.getEnsuredInteraction(null);
        // make sure the ensured interaction is not null, otherwise early return.
        // getEnsuredInteraction will handle the messages.
        if (ensuredInteraction == null)
            return;

        // get the current track
        AudioTrack currentAudioTrack = ensuredInteraction.getAudioSession()
                .getLavaSource()
                .getCurrentPlayingAudioTrack();
        // current track can be null, so make sure it isn't before continuing.
        if (currentAudioTrack == null) {
            this.messageSender.sendNothingPlayingEmbed();
            // early return
            return;
        }

        // send the now playing embed
        this.messageSender.sendNowPlayingEmbed(currentAudioTrack);
    }
}
