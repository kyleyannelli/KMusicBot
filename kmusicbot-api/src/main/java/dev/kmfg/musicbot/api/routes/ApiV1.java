package dev.kmfg.musicbot.api.routes;

import dev.kmfg.musicbot.api.controllers.GuildOverviewController;
import dev.kmfg.musicbot.api.controllers.HealthCheckController;
import dev.kmfg.musicbot.api.controllers.LoginController;
import dev.kmfg.musicbot.api.controllers.TrackedSongController;
import dev.kmfg.musicbot.api.controllers.UserController;
import dev.kmfg.musicbot.api.filters.DiscordOAuthFilter;
import dev.kmfg.musicbot.api.helpers.DiscordOAuthHelper;
import dev.kmfg.musicbot.api.helpers.KMTokens;
import dev.kmfg.musicbot.api.sockets.NowPlayingResponse;
import dev.kmfg.musicbot.api.sockets.SecureWebsocketHandler;
import dev.kmfg.musicbot.database.repositories.DiscordGuildRepo;
import dev.kmfg.musicbot.database.repositories.KMusicSongRepo;
import dev.kmfg.musicbot.database.repositories.SongInitializationRepo;
import dev.kmfg.musicbot.database.repositories.SongPlaytimeRepo;
import dev.kmfg.musicbot.database.repositories.TrackedSongRepo;
import dev.kmfg.musicbot.database.util.HibernateUtil;
import io.github.cdimascio.dotenv.Dotenv;
import spark.Spark;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import balbucio.discordoauth.model.Guild;

public class ApiV1 {
    private static final SessionFactory SESSION_FACTORY = HibernateUtil.getSessionFactory();
    private static final TrackedSongRepo TRACKED_SONG_REPO = new TrackedSongRepo(SESSION_FACTORY);
    private static final DiscordGuildRepo DISCORD_GUILD_REPO = new DiscordGuildRepo(SESSION_FACTORY);
    private static final KMusicSongRepo K_MUSIC_SONG_REPO = new KMusicSongRepo(SESSION_FACTORY);
    private static final SongPlaytimeRepo SONG_PLAYTIME_REPO = new SongPlaytimeRepo(SESSION_FACTORY);
    private static final SongInitializationRepo SONG_INITIALIZATION_REPO = new SongInitializationRepo(SESSION_FACTORY);
    private static final Cache<String, List<Guild>> apiGuildsCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build();

    public static final String CORS_URI = Dotenv.load().get("CORS_URI");
    public static final String COOKIE_URI = Dotenv.load().get("COOKIE_URI");

    private final Logger logger = LoggerFactory.getLogger(ApiV1.class);
    private final ConcurrentHashMap<Long, NowPlayingResponse> guildsNowPlaying;

    public ApiV1(HealthCheckController healthCheckController,
            ConcurrentHashMap<Long, NowPlayingResponse> guildsNowPlaying) {
        this.guildsNowPlaying = guildsNowPlaying;
        this.setupRoutes(healthCheckController);
    }

    public static List<Guild> getCachedGuilds(String refreshToken) {
        return apiGuildsCache.getIfPresent(refreshToken);
    }

    public static void addToGuildsCache(String refreshToken, List<Guild> guilds) {
        apiGuildsCache.put(refreshToken, guilds);
    }

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }

    public static SongPlaytimeRepo getSongPlaytimeRepo() {
        return SONG_PLAYTIME_REPO;
    }

    public static SongInitializationRepo getSongInitRepo() {
        return SONG_INITIALIZATION_REPO;
    }

    public static TrackedSongRepo getTrackedSongRepo() {
        return TRACKED_SONG_REPO;
    }

    public static DiscordGuildRepo getDiscordGuildRepo() {
        return DISCORD_GUILD_REPO;
    }

    public static KMusicSongRepo getKMusicSongRepo() {
        return K_MUSIC_SONG_REPO;
    }

    private void setupRoutes(HealthCheckController healthCheckController) {
        Spark.webSocket("/api/secure/ws", new SecureWebsocketHandler(this.guildsNowPlaying));

        Spark.before("/*", (req, res) -> {
            res.header("Access-Control-Allow-Origin", CORS_URI);
            res.header("Origin", CORS_URI);
            res.header("Access-Control-Allow-Credentials", "true");
            res.header("SameSite", "None");
        });
        Spark.path("/api", () -> {
            // login
            Spark.get("/login", LoginController::login);
            // callback
            Spark.get("/callback", LoginController::callback);
            // logout unsecure
            Spark.get("/logout", LoginController::logout);
        });

        Spark.path("/api/secure", () -> {
            // *****
            // ** TOKEN GETTER
            // *****
            Spark.before("/*", (req, res) -> {
                res.type("application/json");
                logger.debug("BEFORE FILTER ACTIVATED FOR PATH " + req.pathInfo());
                Optional<KMTokens> kmTokens = DiscordOAuthFilter.getCombinedTokens(req);

                if (kmTokens.isEmpty()) {
                    Spark.halt(401);
                    return;
                } else if (!req.pathInfo().toLowerCase().contains("logout")) {
                    req.attribute("km-tokens", kmTokens.get());
                }
            });
            // *****
            // ** TOKEN SETTER
            // *****
            Spark.after("/*", (req, res) -> {
                if (req.attribute("areTokensNew") != null && (boolean) req.attribute("areTokensNew")) {
                    KMTokens kmTokens = (KMTokens) req.attribute("km-tokens");
                    DiscordOAuthHelper.setupCombinedCookies(res, kmTokens);
                } else if (req.attribute("areTokensNew") == null) {
                    KMTokens kmTokens = (KMTokens) req.attribute("km-tokens");
                    DiscordOAuthHelper.setupCombinedCookies(res, kmTokens);
                }
            });

            // *****
            // ** TRACKED SONG CONTROLLER
            // *****
            Spark.get("/tracked-song/:trackedSongId", TrackedSongController::get);

            // *****
            // ** GUILD OVERVIEW CONTROLLER
            // *****
            // get TrackedSongs for a Guild
            Spark.get("/guild/:guildId/tracked-songs", GuildOverviewController::trackedSongs);
            // general stat overview for a Guild
            Spark.get("/guild/:guildId", GuildOverviewController::overview);

            // *****
            // ** LOGIN CONTROLLER
            // *****
            // log out
            Spark.get("/logout", LoginController::logout);

            // *****
            // ** USER CONTROLLER
            // *****
            // route to get yourself
            Spark.get("/me", UserController::me);
            // route to get yourself in a specific guild
            Spark.get("/me/:guildId", UserController::meGuild);
            // route to get guilds you're in
            Spark.get("/guilds", UserController::guilds);
        });
    }
}
