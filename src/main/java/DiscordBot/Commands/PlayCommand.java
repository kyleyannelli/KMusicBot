package DiscordBot.Commands;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import DiscordBot.Sessions.SessionManager;
import Helpers.EnsuredSlashCommandInteraction;
import Helpers.QueueResult;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

public class PlayCommand extends Command {
	public PlayCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
		super(sessionManager, slashCommandEvent, respondLater);
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
			.queueSearchQuery(ensuredInteraction.getParameterValue(songParameter));

		this.messageSender.sendQueueResultEmbed(queueResult);
	}
}
