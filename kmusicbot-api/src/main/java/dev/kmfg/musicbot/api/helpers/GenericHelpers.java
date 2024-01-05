package dev.kmfg.musicbot.api.helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import spark.Request;
import spark.Response;

import dev.kmfg.musicbot.database.repositories.DiscordGuildRepo;
import dev.kmfg.musicbot.api.routes.ApiV1;

public class GenericHelpers {
    public static class Result<A, B> {
        private final A first;
        private final B second;

        private Result(A first, B second) {
            this.first = first;
            this.second = second;
        }

        public static <A, B> Result<A, B> fromFailure(B second) {
            return new Result<A,B>(null, second);
        }

        public static <A, B> Result<A, B> fromSuccess(A first) {
            return new Result<A,B>(first, null);
        }

        public boolean isFail() {
            return this.first == null;
        }

        public boolean isSuccess() {
            return this.first != null;
        }

        public A getSuccess() {
            return this.first;
        }

        public B getFailure() {
            return this.second;
        }
    }

    private static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    private static final Gson gsonUnsafe = new Gson();

    public static boolean isNotNumber(String s) {
        return s == null || s == "" || !s.matches("-?\\d+(\\.\\d+)?");
    }

    public static boolean isNumber(String s) {
        return s != null && s != "" || s.matches("-?\\d+(\\.\\d+)?");
    }

    public static Gson provideUnsafeGson() {
        return gsonUnsafe;
    }

    public static Gson provideGson() {
        return gson;
    }

    public static Result<Long, String> isAuthenticatedAndAvailable(Request req, Response res) {
        String guildId = req.params(":guildId");

        if(GenericHelpers.isNotNumber(guildId)) {
            res.status(400);
            res.type("text");
            return Result.fromFailure("Guild ID is NaN");
        }

        long discordGuildId = Long.parseLong(guildId);

        if(ApiV1.getDiscordGuildRepo().findByDiscordId(discordGuildId).isEmpty()) {
            res.status(404);
            res.type("text");
            return Result.fromFailure("KMusic is not in this guild or hasn't played any songs in this guild.");
        }

        return Result.fromSuccess(discordGuildId);
    }
}
