package dev.kmfg.musicbot.api.controllers;

import java.io.IOException;
import java.util.ArrayList;

import balbucio.discordoauth.DiscordAPI;
import balbucio.discordoauth.model.Guild;
import balbucio.discordoauth.model.User;
import dev.kmfg.musicbot.api.helpers.GenericHelpers;
import dev.kmfg.musicbot.api.helpers.KMTokens;
import dev.kmfg.musicbot.api.routes.ApiV1;
import dev.kmfg.musicbot.api.stateless.models.DiscordGuildIds;
import dev.kmfg.musicbot.api.stateless.models.UserMetrics;
import spark.Request;
import spark.Response;

public class UserController {
    /***
     * Gets total listen time and initializations for a user across all their guilds
     */
    public static String me(Request req, Response res) {
        try {
            KMTokens kmTokens = (KMTokens) req.attribute("km-tokens");
            DiscordAPI discordAPI = new DiscordAPI(kmTokens.getAccessToken());
            User discordAPIUser = discordAPI.fetchUser();
            long discordAPIUserId = Long.valueOf(discordAPIUser.getId());
            long totalUserPlaytime = ApiV1.getSongPlaytimeRepo().getTotalUserPlaytime(discordAPIUserId);
            long totalUserInitializations = ApiV1.getSongInitRepo().getTotalUserInits(discordAPIUserId);
            return GenericHelpers.provideGson().toJson(new UserMetrics(discordAPIUser, totalUserPlaytime, totalUserInitializations));
        }
        catch(IOException ioe) {
            res.status(404);
            return "Not Found";
        }
    }

    public static String guilds(Request req, Response res) {
        try {
            KMTokens kmTokens = (KMTokens) req.attribute("km-tokens");
            DiscordAPI discordAPI = new DiscordAPI(kmTokens.getAccessToken());
            ArrayList<DiscordGuildIds> guilds = new ArrayList<>();
            for(Guild guild : discordAPI.fetchGuilds()) {
                guilds.add(
                        new DiscordGuildIds(guild.getName(), Long.valueOf(guild.getId()))
                        );
            }
            return GenericHelpers.provideGson().toJson(guilds);
        }
        catch(IOException ioe) {
            res.status(404);
            return "Not Found";
        }
    }
}
