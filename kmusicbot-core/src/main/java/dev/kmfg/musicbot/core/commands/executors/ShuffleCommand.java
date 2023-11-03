package dev.kmfg.musicbot.core.commands.executors;

import java.awt.Color;

import dev.kmfg.musicbot.core.util.slashcommands.EnsuredSlashCommandInteraction;
import dev.kmfg.musicbot.core.sessions.AudioSession;
import dev.kmfg.musicbot.core.sessions.SessionManager;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;

public class ShuffleCommand extends Command {
	public static final String COMMAND_NAME = "shuffle";
	private static final String DESCRIPTION = "Shuffles the user added queue. Auto-queue is uneffected.";

	public ShuffleCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent) {
		super(sessionManager, slashCommandEvent);
	}

	@Override
	public void register(DiscordApi discordApi) {
		SlashCommand.with(COMMAND_NAME, DESCRIPTION).createGlobal(discordApi);
	}

	@Override
	public String getCommandName() {
		return this.COMMAND_NAME;
	}

	@Override
	public String getCommandDescription() {
		return this.DESCRIPTION;
	}

	@Override
	public void execute() {
		EnsuredSlashCommandInteraction ensuredInteraction = this.getEnsuredInteraction(null);

		AudioSession audioSession = ensuredInteraction.getAudioSession();
		int totalShuffledTracks = audioSession.getLavaSource().shufflePriorityQueue();

		this.messageSender.getEmbedMessage()
			.setColor(Color.BLACK)
			.setTitle("Shuffled.")
			.setContent(
					new StringBuilder().append(totalShuffledTracks).append(" tracks shuffled.").toString()
					)
			.send();
	}

}
