package dev.kmfg.discordbot.commands;

import dev.kmfg.sessions.SessionManager;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class StopCommand extends Command {
    public StopCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
        super(sessionManager, slashCommandEvent, respondLater);
    }

    @Override
    public void execute() {
        Optional<Server> serverOptional = this.slashCommandEvent.getSlashCommandInteraction().getServer();

        if(serverOptional.isPresent() && isInVoiceChannel(serverOptional.get())) {
            getEnsuredInteraction(null).getAudioSession().properlyDisconnectFromVoiceChannel();
            this.messageSender.sendStoppedEmbed();
        }
        else {
            this.messageSender.sendNothingPlayingEmbed();
            return;
        }
    }

    public boolean isInVoiceChannel(Server server) {
        return server.getAudioConnection().isPresent();
    }
}
