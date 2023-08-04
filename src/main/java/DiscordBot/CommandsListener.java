package DiscordBot;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import DiscordBot.Commands.*;
import DiscordBot.Sessions.SessionManager;
import org.javacord.api.DiscordApi;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

public class CommandsListener {
	private static final String PLAY_COMMAND_NAME = "play";
	private static final String INVITE_COMMAND_NAME = "invite";
	private static final String SKIP_COMMAND_NAME = "skip";
	private static final String STOP_COMMAND_NAME = "stop";
	private static final String VIEW_QUEUE_COMMAND_NAME = "queue";

	private final DiscordApi discordApi;
	private final SessionManager sessionManager;

	public CommandsListener(DiscordApi discordApi, SessionManager sessionManager) {
		this.discordApi = discordApi;
		this.sessionManager = sessionManager;
	}

	public void createAndListenForGlobalCommands() {
		createCommands();
		listenForCommands();
	}

	private void listenForCommands() {
		this.discordApi.addSlashCommandCreateListener(slashCommandEvent -> {
			// we always want to respond later, these commands (could) be quite heavy
			CompletableFuture<InteractionOriginalResponseUpdater> respondLater = slashCommandEvent.getSlashCommandInteraction().respondLater();
			// get slash command name
			String slashCommandName = slashCommandEvent.getSlashCommandInteraction().getCommandName();

			switch(slashCommandName) {
				case PLAY_COMMAND_NAME:
					PlayCommand playCommand = new PlayCommand(this.sessionManager, slashCommandEvent, respondLater);
					playCommand.execute();
					break;
				case INVITE_COMMAND_NAME:
					InviteCommand inviteCommand = new InviteCommand(this.sessionManager, slashCommandEvent, respondLater);
					inviteCommand.execute();
					break;
				case SKIP_COMMAND_NAME:
					SkipCommand skipCommand = new SkipCommand(this.sessionManager, slashCommandEvent, respondLater);
					skipCommand.execute();
					break;
				case STOP_COMMAND_NAME:
					StopCommand stopCommand = new StopCommand(this.sessionManager, slashCommandEvent, respondLater);
					stopCommand.execute();
					break;
				case VIEW_QUEUE_COMMAND_NAME:
					ViewQueueCommand viewQueueCommand = new ViewQueueCommand(this.sessionManager, slashCommandEvent, respondLater);
					viewQueueCommand.execute();
					break;
			}
		});
	}

	private void createCommands() {
		createPlayCommand();
		createInviteCommand();
		createSkipCommand();
		createStopCommand();
		createViewQueueCommand();
	}

	private void createViewQueueCommand() {
		SlashCommand.with(VIEW_QUEUE_COMMAND_NAME, "View the queue of tracks at a specified page number.",
				Collections.singletonList(
						SlashCommandOption
								.create(SlashCommandOptionType.LONG,
										"pageNumber",
										"The page of queue to view.",
										true)
				))
				.createGlobal(this.discordApi);
	}

	private void createSkipCommand() {
		SlashCommand.with(SKIP_COMMAND_NAME, "Skip the current playing song.").createGlobal(this.discordApi);
	}

	private void createInviteCommand() {
		SlashCommand.with(INVITE_COMMAND_NAME, "Get an invite link for the bot").createGlobal(this.discordApi);
	}

	private void createStopCommand() {
		SlashCommand.with(STOP_COMMAND_NAME, "Stop the music, including the queue, then disconnect the bot.").createGlobal(this.discordApi);
	}

	private void createPlayCommand() {
		SlashCommand.with(PLAY_COMMAND_NAME, "Play a song",
				// create option(s)
				Collections.singletonList(
					// create option /play <song>
					SlashCommandOption.create(SlashCommandOptionType.STRING, "song", "The song to play", true)
					))
			.createGlobal(this.discordApi).join();
	}
}
