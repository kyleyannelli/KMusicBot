package dev.kmfg.musicbot.api.controllers;

import java.io.IOException;

import balbucio.discordoauth.DiscordAPI;
import dev.kmfg.musicbot.api.filters.DiscordOAuthFilter;
import spark.Request;
import spark.Response;

public class UserController {
    public static String me(Request req, Response res) {
        try {
            DiscordAPI discordAPI = new DiscordAPI(req.cookie(DiscordOAuthFilter.A_TOKEN));
            String user = discordAPI.fetchUser().toString();
            return user;
        }
        catch(IOException ioe) {
            res.status(404);
            return "Not Found";
        }
    }

    public static String guilds(Request req, Response res) {
        try {
            DiscordAPI discordAPI = new DiscordAPI(req.cookie(DiscordOAuthFilter.A_TOKEN));
            String guilds = discordAPI.fetchGuilds().toString();
            return guilds;
        }
        catch(IOException ioe) {
            res.status(404);
            return "Not Found";
        }
    }
}
