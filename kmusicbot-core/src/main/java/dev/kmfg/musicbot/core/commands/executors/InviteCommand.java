package dev.kmfg.musicbot.core.commands.executors;

import dev.kmfg.musicbot.core.sessions.SessionManager;

import java.util.concurrent.ExecutorService;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;

public class InviteCommand extends Command {
    public static final String COMMAND_NAME = "invite";
    private static final String DESCRIPTION = "Get an invite link for the bot";
    private static final long PERMISSIONS_BITMASK = 36700160L;

    public InviteCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent,
            ExecutorService executorService) {
        super(sessionManager, slashCommandEvent, executorService);
    }

    public InviteCommand() {
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

    @Override
    public void execute() {
        super.execute();
        String inviteLink = this.sessionManager
                .getDiscordApi()
                .createBotInvite(Permissions.fromBitmask(PERMISSIONS_BITMASK));
        String wrappedLink = "[" + "Click Here" + "]" + "(" + inviteLink + ")";
        this.messageSender.sendInviteEmbed(wrappedLink);
    }
}
