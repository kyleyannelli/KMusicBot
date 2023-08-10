package dev.kmfg.discordbot.commands.executors;

import dev.kmfg.helpers.slashcommands.EnsuredSlashCommandInteraction;
import dev.kmfg.helpers.sessions.QueueResult;
import dev.kmfg.sessions.SessionManager;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import java.util.ArrayList;
import java.util.Collections;

public class PlayNextCommand extends Command {
    public static final String COMMAND_NAME = "playnext";
    private static final String DESCRIPTION = "Play a song, but, if there is a queue it will be added to the front.";
    public PlayNextCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent) {
        super(sessionManager, slashCommandEvent);
    }

    @Override
    public void register(DiscordApi discordApi) {
        SlashCommand.with(COMMAND_NAME, DESCRIPTION,
                        // create option(s)
                        Collections.singletonList(
                                // create option /play <song>
                                SlashCommandOption.create(SlashCommandOptionType.STRING, "song", "The song to play", true)
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
