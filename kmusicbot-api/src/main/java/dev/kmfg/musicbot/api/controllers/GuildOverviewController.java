package dev.kmfg.musicbot.api.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dev.kmfg.musicbot.api.helpers.GenericHelpers;
import dev.kmfg.musicbot.api.helpers.GenericHelpers.Result;
import dev.kmfg.musicbot.api.routes.ApiV1;
import dev.kmfg.musicbot.api.stateless.models.DiscordGuildMetrics;
import dev.kmfg.musicbot.database.models.DiscordGuild;
import dev.kmfg.musicbot.database.models.KMusicSong;
import spark.Request;
import spark.Response;

public class GuildOverviewController {
    public static String overview(Request req, Response res) throws IOException {
        Result<DiscordGuild, String> authResult = GenericHelpers.isAuthenticatedAndAvailable(req, res);

        if (authResult.isFail()) {
            return authResult.getFailure();
        }

        long songCount = ApiV1.getTrackedSongRepo().getSongCountByGuild(authResult.getSuccess());
        long playtime = ApiV1.getTrackedSongRepo().getPlaytimeByGuild(authResult.getSuccess());
        return GenericHelpers.provideGson()
                .toJson(
                        new DiscordGuildMetrics(songCount, playtime));
    }

    public static String trackedSongs(Request req, Response res) throws IOException {
        Result<DiscordGuild, String> authResult = GenericHelpers.isAuthenticatedAndAvailable(req, res);

        if (authResult.isFail()) {
            return authResult.getFailure();
        }

        List<Long> filterUserIds = new ArrayList<>();
        int size = 20, page = 0;
        String search = null;
        String order = "DESC";

        if (req.queryParamsValues("order") != null) {
            order = req.queryParamsValues("order")[0];
        }

        if (req.queryParamsValues("userId") != null) {
            for (String userId : req.queryParamsValues("userId")) {
                if (GenericHelpers.isNotNumber(userId))
                    continue;
                filterUserIds.add(Long.parseLong(userId));
            }
        }

        if (req.queryParamsValues("size") != null && req.queryParamsValues("size")[0] != null
                && GenericHelpers.isNumber(req.queryParamsValues("size")[0])) {
            size = Integer.valueOf(req.queryParamsValues("size")[0]);
        }

        if (size > 100 || size < 1) {
            res.status(400);
            return "Page size cannot exceed 100 items!";
        }

        if (req.queryParamsValues("page") != null && req.queryParamsValues("page")[0] != null
                && GenericHelpers.isNumber(req.queryParamsValues("page")[0])) {
            page = Integer.valueOf(req.queryParamsValues("page")[0]);
        }

        if (req.queryParamsValues("page") != null && req.queryParamsValues("page")[0] != null
                && GenericHelpers.isNumber(req.queryParamsValues("page")[0])) {
            page = Integer.valueOf(req.queryParamsValues("page")[0]);
        }

        if (req.queryParamsValues("search") != null && req.queryParamsValues("search")[0] != null
                && req.queryParamsValues("search")[0].length() <= Math
                        .max(KMusicSong.MAX_TITLE_LENGTH, KMusicSong.MAX_AUTHOR_LENGTH)) {
            search = req.queryParamsValues("search")[0];
        }

        if (search == null) {
            return GenericHelpers
                    .provideGson()
                    .toJson(
                            ApiV1.getTrackedSongRepo()
                                    .findByDiscordGuildId(
                                            authResult.getSuccess().getDiscordId(),
                                            page,
                                            size,
                                            filterUserIds,
                                            order));
        } else {
            return GenericHelpers
                    .provideGson()
                    .toJson(
                            ApiV1.getTrackedSongRepo()
                                    .findByDiscordGuildIdAndSearchQuery(
                                            authResult.getSuccess().getDiscordId(),
                                            page,
                                            size,
                                            search,
                                            filterUserIds,
                                            order));
        }
    }
}
