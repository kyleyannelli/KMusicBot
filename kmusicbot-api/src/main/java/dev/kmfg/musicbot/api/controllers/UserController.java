package dev.kmfg.musicbot.api.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import balbucio.discordoauth.DiscordAPI;
import balbucio.discordoauth.model.Guild;
import balbucio.discordoauth.model.User;
import dev.kmfg.musicbot.api.helpers.GenericHelpers;
import dev.kmfg.musicbot.api.helpers.KMTokens;
import dev.kmfg.musicbot.api.routes.ApiV1;
import dev.kmfg.musicbot.api.stateless.models.DiscordGuildIds;
import dev.kmfg.musicbot.api.stateless.models.DiscordGuildUserPlaytimeMetric;
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
            List<DiscordGuildUserPlaytimeMetric> guildPlaytimes = new ArrayList<>();
            for(Guild guild : discordAPI.fetchGuilds()) {
                long guildId = Long.valueOf(guild.getId());
                guildPlaytimes.add(
                        new DiscordGuildUserPlaytimeMetric(guild.getName(),
                            guildId,
                            ApiV1.getSongPlaytimeRepo().getTotalUserPlaytime(discordAPIUserId, guildId),
                            ApiV1.getSongInitRepo().getTotalUserInits(discordAPIUserId, guildId)
                        )
                );
            }
            return GenericHelpers.provideGson().toJson(new UserMetrics(discordAPIUser, totalUserPlaytime, totalUserInitializations, guildPlaytimes));
        }
        catch(IOException ioe) {
            res.status(404);
            return "Not Found";
        }
    }

    /***
     * Gets total listen time and initializations for a user for SPECIFIC GUILD
     */
    public static String meGuild(Request req, Response res) {
        try {
            KMTokens kmTokens = (KMTokens) req.attribute("km-tokens");
            DiscordAPI discordAPI = new DiscordAPI(kmTokens.getAccessToken());
            User discordAPIUser = discordAPI.fetchUser();
            long discordAPIUserId = Long.valueOf(discordAPIUser.getId());
            long discordAPIGuildId = Long.valueOf(req.params(":guildId"));
            long totalUserPlaytime = ApiV1.getSongPlaytimeRepo().getTotalUserPlaytime(discordAPIUserId, discordAPIGuildId);
            long totalUserInitializations = ApiV1.getSongInitRepo().getTotalUserInits(discordAPIUserId, discordAPIGuildId);
            return GenericHelpers
                .provideGson()
                .toJson(new UserMetrics(discordAPIUser, totalUserPlaytime, totalUserInitializations));
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
