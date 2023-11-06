package dev.kmfg.musicbot.core.listenerhandlers;

import dev.kmfg.musicbot.core.commands.intermediates.CommandsRegistry;
import dev.kmfg.musicbot.core.sessions.SessionManager;
import dev.kmfg.musicbot.core.commands.executors.Command;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
		if(command != null) {
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
}
