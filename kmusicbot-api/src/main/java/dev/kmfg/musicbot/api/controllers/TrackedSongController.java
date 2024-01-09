package dev.kmfg.musicbot.api.controllers;

import java.io.IOException;
import java.util.Optional;

import balbucio.discordoauth.DiscordAPI;
import balbucio.discordoauth.model.User;
import dev.kmfg.musicbot.api.helpers.GenericHelpers;
import dev.kmfg.musicbot.api.helpers.KMTokens;
import dev.kmfg.musicbot.api.routes.ApiV1;
import dev.kmfg.musicbot.database.models.TrackedSong;
import spark.Request;
import spark.Response;

public class TrackedSongController {
    public static String get(Request req, Response res) throws IOException {
        KMTokens kmTokens = (KMTokens) req.attribute("km-tokens");
        DiscordAPI discordAPI = new DiscordAPI(kmTokens.getAccessToken());
        User user = discordAPI.fetchUser();

        Optional<TrackedSong> trackedSong = ApiV1.getTrackedSongRepo().findById(Integer.valueOf(req.params(":trackedSongId")));

        if(trackedSong.isEmpty()) {
            res.type("text");
            res.status(404);
            return "Not Found.";
        }

        if(!GenericHelpers.isUserInGuild(req, trackedSong.get().getGuild().getDiscordId())) {
            res.type("text");
            res.status(404);
            return "Not Found.";
        }

        return GenericHelpers.provideGson().toJson(trackedSong.get()); 
    }
}
