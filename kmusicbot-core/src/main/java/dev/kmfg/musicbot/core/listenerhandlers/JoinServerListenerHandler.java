package dev.kmfg.musicbot.core.listenerhandlers;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.permission.RoleBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.ServerJoinEvent;
import org.javacord.api.listener.server.ServerJoinListener;

import java.awt.*;

/**
 * Handles the initialization of a server that the bot joins.
 */
public class JoinServerListenerHandler implements ServerJoinListener {

    public JoinServerListenerHandler(DiscordApi discordApi) {
        // Initialize each server the bot is in to create the DJ role
        for (Server server : discordApi.getServers())
        {
            if (server.getRolesByName("DJ").isEmpty())
                this.setupServer(server);
        }
    }
    public void onServerJoin(ServerJoinEvent serverJoinEvent) {
        this.setupServer(serverJoinEvent.getServer());
    }

    /**
     * Setup each server with a DJ role giving users permission to use the bot.
     */
    protected void setupServer(Server server) {
        RoleBuilder roleBuilder = server.createRoleBuilder();

        roleBuilder
                .setName("DJ")
                .setMentionable(true)
                .setColor(Color.blue)
                .create();
    }
}
