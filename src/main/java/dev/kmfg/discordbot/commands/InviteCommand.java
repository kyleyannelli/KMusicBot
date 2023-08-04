package dev.kmfg.discordbot.commands;

import dev.kmfg.sessions.SessionManager;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

import java.util.concurrent.CompletableFuture;

public class InviteCommand extends Command {
    private static final long PERMISSIONS_BITMASK = 36700160L;
    public InviteCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
        super(sessionManager, slashCommandEvent, respondLater);
    }

    @Override
    public void execute() {
        String inviteLink = this.sessionManager
                .getDiscordApi()
                .createBotInvite(Permissions.fromBitmask(PERMISSIONS_BITMASK));
        this.messageSender.sendInviteEmbed(inviteLink);
    }
}
