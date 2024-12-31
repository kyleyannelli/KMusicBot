package dev.kmfg.musicbot.api.helpers;

import java.security.SecureRandom;

import balbucio.discordoauth.DiscordOAuth;
import balbucio.discordoauth.model.TokensResponse;
import dev.kmfg.musicbot.api.filters.DiscordOAuthFilter;
import dev.kmfg.musicbot.api.routes.ApiV1;
import io.github.cdimascio.dotenv.Dotenv;
import spark.Response;

public class DiscordOAuthHelper {
    private static final int COOKIE_EXPIRE_SECONDS = 3600 * 24 * 7;

    private static final String[] SCOPES = {
            "identify",
            "guilds"
    };
    private static final long STATE_STR_LEN = 25;

    private static final Dotenv dotenv = Dotenv.load();
    private static final String clientID = dotenv.get("DISCORD_CLIENT_ID");
    private static final String clientSecret = dotenv.get("DISCORD_CLIENT_SECRET");
    private static final String redirectUri = dotenv.get("DISCORD_REDIRECT_URI");
    private static final DiscordOAuth discordOAuth = new DiscordOAuth(clientID, clientSecret, redirectUri, SCOPES);

    public static DiscordOAuth getOAuth() {
        return discordOAuth;
    }

    /**
     * Generates Discord Authorization URL.
     * Position 0 is the URL & Position 1 is the State String
     */
    public static String[] generateAuthUrl(String returnUrl) {
        return new String[] { discordOAuth.getAuthorizationURL(returnUrl), returnUrl };
    }

    /**
     * Generates Discord Authorization URL.
     * Position 0 is the URL & Position 1 is the State String
     */
    public static String[] generateAuthUrl() {
        String state = generateRandom();
        return new String[] { discordOAuth.getAuthorizationURL(""), state };
    }

    /**
     * Generate a true random String of length STATE_STR_LEN using SecureRandom
     */
    public static String generateRandom() {
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < STATE_STR_LEN; i++) {
            char c = (char) (33 + secureRandom.nextInt(126 - 33));
            stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }

    @Deprecated
    public static void setupCookies(Response res, TokensResponse tokens) throws Exception {
        String[] accessTokenAndSalt = CryptoHelper.encrypt(tokens.getAccessToken());
        String[] refreshTokenAndSalt = CryptoHelper.encrypt(tokens.getRefreshToken());
        res.cookie(ApiV1.COOKIE_URI, "/", DiscordOAuthFilter.A_TOKEN, accessTokenAndSalt[0], COOKIE_EXPIRE_SECONDS,
                true, true);
        res.cookie(ApiV1.COOKIE_URI, "/", DiscordOAuthFilter.R_TOKEN, refreshTokenAndSalt[0], COOKIE_EXPIRE_SECONDS,
                true, true);
        res.cookie(ApiV1.COOKIE_URI, "/", DiscordOAuthFilter.A_SALT, accessTokenAndSalt[1], COOKIE_EXPIRE_SECONDS, true,
                true);
        res.cookie(ApiV1.COOKIE_URI, "/", DiscordOAuthFilter.R_SALT, refreshTokenAndSalt[1], COOKIE_EXPIRE_SECONDS,
                true, true);
    }

    @Deprecated
    public static void setupCookies(Response res, KMTokens tokens) throws Exception {
        String[] accessTokenAndSalt = CryptoHelper.encrypt(tokens.getAccessToken());
        String[] refreshTokenAndSalt = CryptoHelper.encrypt(tokens.getRefreshToken());
        res.cookie(ApiV1.COOKIE_URI, "/", DiscordOAuthFilter.A_TOKEN, accessTokenAndSalt[0], COOKIE_EXPIRE_SECONDS,
                true, true);
        res.cookie(ApiV1.COOKIE_URI, "/", DiscordOAuthFilter.R_TOKEN, refreshTokenAndSalt[0], COOKIE_EXPIRE_SECONDS,
                true, true);
        res.cookie(ApiV1.COOKIE_URI, "/", DiscordOAuthFilter.A_SALT, accessTokenAndSalt[1], COOKIE_EXPIRE_SECONDS, true,
                true);
        res.cookie(ApiV1.COOKIE_URI, "/", DiscordOAuthFilter.R_SALT, refreshTokenAndSalt[1], COOKIE_EXPIRE_SECONDS,
                true, true);
    }

    public static void setupCombinedCookies(Response res, KMTokens tokens) throws Exception {
        String combinedTokens = tokens.getAccessToken() + ":" + tokens.getRefreshToken();
        String[] encryptedTokenAndSalt = CryptoHelper.encrypt(combinedTokens);

        res.cookie(ApiV1.COOKIE_URI, "/", DiscordOAuthFilter.COMBINED_TOKEN, encryptedTokenAndSalt[0],
                COOKIE_EXPIRE_SECONDS,
                true, true);
        res.cookie(ApiV1.COOKIE_URI, "/", DiscordOAuthFilter.COMBINED_SALT, encryptedTokenAndSalt[1],
                COOKIE_EXPIRE_SECONDS,
                true, true);
    }

    public static void setupCombinedCookies(Response res, TokensResponse tokens) throws Exception {
        String combinedTokens = tokens.getAccessToken() + ":" + tokens.getRefreshToken();
        String[] encryptedTokenAndSalt = CryptoHelper.encrypt(combinedTokens);

        res.cookie(ApiV1.COOKIE_URI, "/", DiscordOAuthFilter.COMBINED_TOKEN, encryptedTokenAndSalt[0],
                COOKIE_EXPIRE_SECONDS,
                true, true);
        res.cookie(ApiV1.COOKIE_URI, "/", DiscordOAuthFilter.COMBINED_SALT, encryptedTokenAndSalt[1],
                COOKIE_EXPIRE_SECONDS,
                true, true);
    }
}
