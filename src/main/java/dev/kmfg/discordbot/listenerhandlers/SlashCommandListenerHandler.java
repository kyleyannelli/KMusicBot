package dev.kmfg.discordbot.listenerhandlers;

import dev.kmfg.discordbot.commands.executors.Command;
import dev.kmfg.discordbot.commands.intermediates.CommandsRegistry;
import dev.kmfg.sessions.SessionManager;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;

public class SlashCommandListenerHandler implements SlashCommandCreateListener {
	private final SessionManager sessionManager;
	private final CommandsRegistry commandsRegistry;

	public SlashCommandListenerHandler(SessionManager sessionManager) {
		this.sessionManager = sessionManager;
		this.commandsRegistry = new CommandsRegistry();
	}

	@Override
	public void onSlashCommandCreate(SlashCommandCreateEvent event) {
		// get slash command name
		String slashCommandName = event.getSlashCommandInteraction().getCommandName();

		Command command = this.commandsRegistry.getCommand(slashCommandName, sessionManager, event);
		if(command != null) {
			command.execute();
		}
	}
}
