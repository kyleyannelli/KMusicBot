package dev.kmfg.musicbot.api.routes;

import dev.kmfg.musicbot.api.controllers.GuildOverviewController;
import dev.kmfg.musicbot.api.controllers.HealthCheckController;
import dev.kmfg.musicbot.api.controllers.LoginController;
import dev.kmfg.musicbot.api.controllers.UserController;
import dev.kmfg.musicbot.api.filters.DiscordOAuthFilter;
import dev.kmfg.musicbot.api.helpers.DiscordOAuthHelper;
import dev.kmfg.musicbot.api.helpers.KMTokens;
import dev.kmfg.musicbot.database.repositories.DiscordGuildRepo;
import dev.kmfg.musicbot.database.repositories.KMusicSongRepo;
import dev.kmfg.musicbot.database.repositories.SongInitializationRepo;
import dev.kmfg.musicbot.database.repositories.SongPlaytimeRepo;
import dev.kmfg.musicbot.database.repositories.TrackedSongRepo;
import dev.kmfg.musicbot.database.util.HibernateUtil;
import spark.Spark;

import java.util.Optional;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiV1 {
    private static final SessionFactory SESSION_FACTORY = HibernateUtil.getSessionFactory();
    private static final TrackedSongRepo TRACKED_SONG_REPO = new TrackedSongRepo(SESSION_FACTORY);
    private static final DiscordGuildRepo DISCORD_GUILD_REPO = new DiscordGuildRepo(SESSION_FACTORY);
    private static final KMusicSongRepo K_MUSIC_SONG_REPO = new KMusicSongRepo(SESSION_FACTORY);
    private static final SongPlaytimeRepo SONG_PLAYTIME_REPO = new SongPlaytimeRepo(SESSION_FACTORY);
    private static final SongInitializationRepo SONG_INITIALIZATION_REPO = new SongInitializationRepo(SESSION_FACTORY);

    private final Logger logger = LoggerFactory.getLogger(ApiV1.class);

    public ApiV1(HealthCheckController healthCheckController) {
        this.setupRoutes(healthCheckController);
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
        Spark.path("/api", () -> {
            // login
            Spark.get("/login", LoginController::login);
            // callback
            Spark.get("/callback", LoginController::callback);
            // logout unsecure
            Spark.get("/logout", LoginController::logout);
        });

        Spark.path("/api/secure", () -> {
            //*****
            //** TOKEN GETTTTAAAA
            //*****
            Spark.before("/*", (req, res) -> {
                // make sure we are responding in json format
                res.type("application/json");
                logger.debug("BEFORE FILTER ACTIVATED FOR PATH " + req.pathInfo());
                Optional<KMTokens> kmTokens = DiscordOAuthFilter.getTokens(req);
                if(kmTokens.isEmpty()) {
                    LoginController.login(req, res, req.pathInfo());
                    return;
                }
                else if(!req.pathInfo().toLowerCase().contains("logout")) {
                    //DiscordOAuthHelper.setupCookies(res, kmTokens.get());
                    req.attribute("km-tokens", kmTokens.get());
                }
            });
            //*****
            //** TOKEN SETTA
            //*****
            Spark.after("/*", (req, res) -> {
                if(req.attribute("areTokensNew") != null && (boolean) req.attribute("areTokensNew")) {
                    KMTokens kmTokens = (KMTokens) req.attribute("km-tokens");
                    DiscordOAuthHelper.setupCookies(res, kmTokens);
                }
                else if(req.attribute("areTokensNew") == null) {
                    KMTokens kmTokens = (KMTokens) req.attribute("km-tokens");
                    DiscordOAuthHelper.setupCookies(res, kmTokens);
                }
            });

            //*****
            //** GUILD OVERVIEW CONTROLLER
            //*****
            // get TrackedSongs for a Guild
            Spark.get("/guild/:guildId/tracked-songs", GuildOverviewController::trackedSongs);
            // general stat overview for a Guild
            Spark.get("/guild/:guildId", GuildOverviewController::overview);

            //*****
            //** LOGIN CONTROLLER
            //*****
            // log out
            Spark.get("/logout", LoginController::logout);

            //*****
            //** USER CONTROLLER
            //*****
            // route to get yourself
            Spark.get("/me", UserController::me);
            // route to get guilds you're in 
            Spark.get("/guilds", UserController::guilds);
        });
    }
}
