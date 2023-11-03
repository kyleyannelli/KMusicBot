package dev.kmfg.musicbot.core.commands.executors;

import java.util.ArrayList;
import java.util.Collections;

import dev.kmfg.musicbot.core.util.slashcommands.EnsuredSlashCommandInteraction;
import dev.kmfg.musicbot.core.sessions.SessionManager;
import dev.kmfg.musicbot.core.util.sessions.QueueResult;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

public class PlayCommand extends Command {
	public static final String COMMAND_NAME = "play";
	private static final String DESCRIPTION = "Play a song";
	public PlayCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent) {
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
			.queueSearchQuery(this.discordUser, ensuredInteraction.getParameterValue(songParameter));

		this.messageSender.sendQueueResultEmbed(queueResult);
	}
}
