package dev.kmfg.musicbot.api.controllers;

import java.io.IOException;

import balbucio.discordoauth.DiscordAPI;
import dev.kmfg.musicbot.api.helpers.GenericHelpers;
import dev.kmfg.musicbot.api.helpers.KMTokens;
import spark.Request;
import spark.Response;

public class UserController {
    public static String me(Request req, Response res) {
        try {
            KMTokens kmTokens = (KMTokens) req.attribute("km-tokens");
            DiscordAPI discordAPI = new DiscordAPI(kmTokens.getAccessToken());
            return GenericHelpers.provideUnsafeGson().toJson(discordAPI.fetchUser());
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
            return GenericHelpers.provideUnsafeGson().toJson(discordAPI.fetchGuilds());
        }
        catch(IOException ioe) {
            res.status(404);
            return "Not Found";
        }
    }
}
