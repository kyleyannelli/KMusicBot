package dev.kmfg.musicbot.api.controllers;

import dev.kmfg.musicbot.api.helpers.GenericHelpers;
import dev.kmfg.musicbot.api.helpers.GenericHelpers.Result;
import dev.kmfg.musicbot.api.routes.ApiV1;
import spark.Request;
import spark.Response;

public class GuildOverviewController {
    public static String overview(Request req, Response res) {
        return null;
    }

    public static String trackedSongs(Request req, Response res) {
        Result<Long, String> authResult = GenericHelpers.isAuthenticatedAndAvailable(req, res);

        if(authResult.isFail()) {
            return authResult.getFailure();
        }

        return GenericHelpers
            .provideGson()
            .toJson(
                ApiV1.getTrackedSongRepo().findByDiscordGuildId(authResult.getSuccess()
            )
        );
    }
}

