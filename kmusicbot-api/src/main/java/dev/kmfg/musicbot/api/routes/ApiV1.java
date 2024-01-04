package dev.kmfg.musicbot.api.routes;

import dev.kmfg.musicbot.api.controllers.GuildOverviewController;
import dev.kmfg.musicbot.api.controllers.HealthCheckController;
import dev.kmfg.musicbot.api.controllers.LoginController;
import dev.kmfg.musicbot.api.controllers.UserController;
import dev.kmfg.musicbot.api.filters.DiscordOAuthFilter;
import dev.kmfg.musicbot.api.helpers.DiscordOAuthHelper;
import dev.kmfg.musicbot.api.helpers.KMTokens;

import spark.Spark;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiV1 {
    private final Logger logger = LoggerFactory.getLogger(ApiV1.class);

    public ApiV1(HealthCheckController healthCheckController) {
        this.setupRoutes(healthCheckController);
    }

    private void setupRoutes(HealthCheckController healthCheckController) {
        Spark.path("/api", () -> {
            // login
            Spark.get("/login", LoginController::login);
            // callback
            Spark.get("/callback", LoginController::callback);
        });

        Spark.path("/api/secure", () -> {
            //*****
            //** TOKEN GETTTTAAAA
            //*****
            Spark.before("/*", (req, res) -> {
                logger.debug("BEFORE FILTER ACTIVATED FOR PATH " + req.pathInfo());
                Optional<KMTokens> kmTokens = DiscordOAuthFilter.getTokens(req);
                if(kmTokens.isEmpty()) {
                    LoginController.login(req, res, req.pathInfo());
                    return;
                }
                else if(!req.pathInfo().toLowerCase().contains("logout")){
                    logger.debug("Updated cookies!");
                    DiscordOAuthHelper.setupCookies(res, kmTokens.get());
                }
            });

            //*****
            //** GUILD OVERVIEW CONTROLLER
            //*****
            // get TrackedSongs for a Guild
            Spark.get("/guild/:guildId/tracked-songs", GuildOverviewController::trackedSongs);

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
