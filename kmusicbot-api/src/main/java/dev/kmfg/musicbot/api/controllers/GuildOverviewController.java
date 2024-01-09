package dev.kmfg.musicbot.api.controllers;

import java.io.IOException;

import dev.kmfg.musicbot.api.helpers.GenericHelpers;
import dev.kmfg.musicbot.api.helpers.GenericHelpers.Result;
import dev.kmfg.musicbot.api.routes.ApiV1;
import dev.kmfg.musicbot.api.stateless.models.DiscordGuildMetrics;
import dev.kmfg.musicbot.database.models.DiscordGuild;
import spark.Request;
import spark.Response;

public class GuildOverviewController {
    public static String overview(Request req, Response res) throws IOException {
        Result<DiscordGuild, String> authResult = GenericHelpers.isAuthenticatedAndAvailable(req, res);

        if(authResult.isFail()) {
            return authResult.getFailure();
        }

        long songCount = ApiV1.getTrackedSongRepo().getSongCountByGuild(authResult.getSuccess());
        long playtime = ApiV1.getTrackedSongRepo().getPlaytimeByGuild(authResult.getSuccess());
        return GenericHelpers.provideGson()
            .toJson(
                    new DiscordGuildMetrics(songCount, playtime)
            );
    }

    public static String trackedSongs(Request req, Response res) throws IOException {
        Result<DiscordGuild, String> authResult = GenericHelpers.isAuthenticatedAndAvailable(req, res);

        if(authResult.isFail()) {
            return authResult.getFailure();
        }

        int size = 20, page = 0;

        if(req.queryParamsValues("size") != null && req.queryParamsValues("size")[0] != null && GenericHelpers.isNumber(req.queryParamsValues("size")[0])) {
            size = Integer.valueOf(req.queryParamsValues("size")[0]);
        }

        if(req.queryParamsValues("page") != null && req.queryParamsValues("page")[0] != null && GenericHelpers.isNumber(req.queryParamsValues("page")[0])) {
            page = Integer.valueOf(req.queryParamsValues("page")[0]);
        }

        return GenericHelpers
            .provideGson()
            .toJson(
                ApiV1.getTrackedSongRepo()
                .findByDiscordGuildId(
                    authResult.getSuccess().getDiscordId(),
                    page,
                    size
                )
        );
    }
}

