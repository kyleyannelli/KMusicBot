package dev.kmfg.musicbot.api.controllers;

import java.io.IOException;
import java.util.Optional;

import dev.kmfg.musicbot.api.helpers.GenericHelpers;
import dev.kmfg.musicbot.api.routes.ApiV1;
import dev.kmfg.musicbot.database.models.TrackedSong;
import spark.Request;
import spark.Response;

public class TrackedSongController {
    public static String get(Request req, Response res) throws IOException {
        Optional<TrackedSong> trackedSong = ApiV1.getTrackedSongRepo().findById(Integer.valueOf(req.params(":trackedSongId")));

        if(trackedSong.isEmpty() || !GenericHelpers.isUserInGuild(req, trackedSong.get().getGuild().getDiscordId())) {
            res.type("text");
            res.status(404);
            return "Not Found.";
        }

        return GenericHelpers.provideGson().toJson(trackedSong.get()); 
    }
}
