package dev.kmfg.musicbot.api.controllers;

import java.io.IOException;

import balbucio.discordoauth.DiscordAPI;
import dev.kmfg.musicbot.api.filters.DiscordOAuthFilter;
import dev.kmfg.musicbot.api.helpers.GenericHelpers;
import spark.Request;
import spark.Response;

public class UserController {
    public static String me(Request req, Response res) {
        try {
            DiscordAPI discordAPI = new DiscordAPI(req.cookie(DiscordOAuthFilter.A_TOKEN));
            return GenericHelpers.provideUnsafeGson().toJson(discordAPI.fetchUser());
        }
        catch(IOException ioe) {
            res.status(404);
            return "Not Found";
        }
    }

    public static String guilds(Request req, Response res) {
        try {
            DiscordAPI discordAPI = new DiscordAPI(req.cookie(DiscordOAuthFilter.A_TOKEN));
            return GenericHelpers.provideUnsafeGson().toJson(discordAPI.fetchGuilds());
        }
        catch(IOException ioe) {
            res.status(404);
            return "Not Found";
        }
    }
}
