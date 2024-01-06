package dev.kmfg.musicbot.api.helpers;

import java.io.IOException;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import balbucio.discordoauth.DiscordAPI;
import balbucio.discordoauth.model.Guild;
import spark.Request;
import spark.Response;

import dev.kmfg.musicbot.api.routes.ApiV1;
import dev.kmfg.musicbot.database.models.DiscordGuild;

public class GenericHelpers {
    private static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    private static final Gson gsonUnsafe = new Gson();

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

    public static Result<DiscordGuild, String> isAuthenticatedAndAvailable(Request req, Response res) throws IOException {
        String guildId = req.params(":guildId");

        if(GenericHelpers.isNotNumber(guildId)) {
            res.status(400);
            res.type("text");
            return Result.fromFailure("Guild ID is NaN");
        }

        long discordGuildId = Long.parseLong(guildId);

        if(!isUserInGuild(req, discordGuildId)) {
            res.status(403);
            res.type("text");
            return Result.fromFailure("User is not in this Guild!");
        }

        Optional<DiscordGuild> guild = ApiV1.getDiscordGuildRepo().findByDiscordId(discordGuildId);
        if(guild.isEmpty()) {
            res.status(404);
            res.type("text");
            return Result.fromFailure("KMusic is not in this guild or hasn't played any songs in this guild.");
        }

        return Result.fromSuccess(guild.get());
    }

    private static boolean isUserInGuild(Request req, long guildId) throws IOException {
        KMTokens kmTokens = (KMTokens) req.attribute("km-tokens");

        for(Guild guild : new DiscordAPI(kmTokens.getAccessToken()).fetchGuilds()) {
            if(Long.valueOf(guild.getId()) == guildId) return true;
        }

        return false;
    }
}
