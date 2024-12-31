package dev.kmfg.musicbot.core.commands.executors;

import dev.kmfg.musicbot.core.sessions.SessionManager;

import dev.kmfg.musicbot.core.util.slashcommands.EnsuredSlashCommandInteraction;
import dev.kmfg.musicbot.core.lavaplayer.AudioTrackWithUser;
import dev.kmfg.musicbot.core.sessions.AudioSession;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

public class RemoveCommand extends Command {
    public static final String COMMAND_NAME = "remove";
    private static final String DESCRIPTION = "By number, removes specified song from queue.";

    public RemoveCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent,
            ExecutorService executorService) {
        super(sessionManager, slashCommandEvent, executorService);
    }

    public RemoveCommand() {
        super();
    }

    @Override
    public void register(DiscordApi discordApi) {
        SlashCommand.with(COMMAND_NAME, DESCRIPTION,
                // create option(s)
                Collections.singletonList(
                        // create option /play <song>
                        SlashCommandOption.create(SlashCommandOptionType.LONG, "position",
                                "The song by position in queue to remove.", true)))
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
        super.execute();
        String requiredNumber = "position";
        ArrayList<String> params = new ArrayList<>();
        params.add(requiredNumber);

        EnsuredSlashCommandInteraction ensuredInteraction = this.getEnsuredInteraction(params);

        AudioSession audioSession = ensuredInteraction.getAudioSession();

        int songPosition;

        try {
            songPosition = Integer.parseInt(ensuredInteraction.getParameterValue(requiredNumber));
            if (songPosition < 0 || songPosition > audioSession.getAudioQueue().size()) {
                this.messageSender.sendBadParameterEmbed(requiredNumber);
                return;
            }
        } catch (Exception e) {
            Logger.warn(e, "Error occurred while attempting to remove song by position. Parameter failed to parse!");
            this.messageSender.sendBadParameterEmbed(requiredNumber);
            return;
        }

        Optional<AudioTrackWithUser> audioTrackWithUser = audioSession.remove(songPosition);

        AudioTrackWithUser removedTrack;

        if (audioTrackWithUser.isEmpty()) {
            Logger.warn("Audio Track empty during attempted remove!");
            this.messageSender.sendNothingFoundEmbed("" + songPosition);
            return;
        }

        removedTrack = audioTrackWithUser.get();

        this.messageSender.sendRemovedEmbed(removedTrack);
        return;
    }
}
