package dev.kmfg.musicbot.api.controllers;

import dev.kmfg.musicbot.api.helpers.GenericHelpers;
import spark.Request;
import spark.Response;

public class GuildOverviewController {
    public static String trackedSongs(Request req, Response res) {
        String guildId = req.params(":guildId");

        if(GenericHelpers.isNotNumber(guildId)) {
            res.status(400);
            return "Invalid Guild ID";
        }

        return guildId;
    }
}

