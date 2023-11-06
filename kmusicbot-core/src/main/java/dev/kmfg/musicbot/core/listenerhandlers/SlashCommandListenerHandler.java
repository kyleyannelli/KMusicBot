package dev.kmfg.musicbot.core.listenerhandlers;

import dev.kmfg.musicbot.core.commands.intermediates.CommandsRegistry;
import dev.kmfg.musicbot.core.sessions.SessionManager;
import dev.kmfg.musicbot.core.commands.executors.Command;
import io.github.cdimascio.dotenv.Dotenv;

import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;
import org.tinylog.Logger;

public class SlashCommandListenerHandler implements SlashCommandCreateListener {
	// by default allow 10 threads for the pool
	private final static int DEFAULT_MAX_COMMAND_THREADS = 10;
	private final SessionManager sessionManager;
	private final CommandsRegistry commandsRegistry;
	private final ExecutorService commandExecutorService;

	public SlashCommandListenerHandler(SessionManager sessionManager, CommandsRegistry commandsRegistry) {
		this.sessionManager = sessionManager;
		this.commandsRegistry = commandsRegistry;

		Dotenv.load().get("MAX_COMMAND_THREADS");

		this.commandExecutorService = createCommandExecutorService();
	}

	@Override
	public void onSlashCommandCreate(SlashCommandCreateEvent event) {
		// get slash command name
		String slashCommandName = event.getSlashCommandInteraction().getCommandName();

		Command command = this.commandsRegistry.getCommand(slashCommandName, sessionManager, event);
		if(command != null && userHasPermission(event)) {
			commandExecutorService.submit((() -> { command.execute(); }));
		}
	}

	protected ExecutorService createCommandExecutorService() {
		try {
			return Dotenv.load().get("MAX_COMMAND_THREADS") == null ?
				Executors.newFixedThreadPool(DEFAULT_MAX_COMMAND_THREADS) :
				Executors.newFixedThreadPool(
						Integer.parseInt(Dotenv.load().get("MAX_COMMAND_THREADS"))
						);
		}
		catch (NumberFormatException numberFormatException) {
			Logger.error("Attempted to parse the number of threads for MAX_COMMAND_THREADS, but got an error instead...\n\tCheck it is an integer in your .env");
			System.exit(1);
			throw numberFormatException;
		}
	}

	protected boolean userHasPermission(SlashCommandCreateEvent event) {
		User user = event.getInteraction().getUser();
		//noinspection OptionalGetWithoutIsPresent
		Role djRole = event.getInteraction().getServer().get().getRolesByName("DJ").get(0);

		if (user.getRoles(event.getInteraction().getServer().get()).contains(djRole))
			return true;

		event.getInteraction().createImmediateResponder().respond().thenAccept(action -> {
			action.addEmbed(new EmbedBuilder()
					.addField("Permission denied", "You must have the " + djRole.getMentionTag() + " role I created", false))
					.update();
		});

		return false;
	}
}
