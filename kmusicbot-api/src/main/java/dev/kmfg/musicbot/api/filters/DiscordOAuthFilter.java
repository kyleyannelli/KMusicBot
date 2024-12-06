package dev.kmfg.musicbot.api.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import balbucio.discordoauth.model.TokensResponse;
import balbucio.discordoauth.DiscordAPI;
import dev.kmfg.musicbot.api.helpers.CryptoHelper;
import dev.kmfg.musicbot.api.helpers.DiscordOAuthHelper;
import dev.kmfg.musicbot.api.helpers.KMTokens;

import spark.Request;

import java.util.Optional;
import java.io.IOException;

public class DiscordOAuthFilter {
    public final static String A_TOKEN = "access-token";
    public final static String R_TOKEN = "refresh-token";
    public final static String A_SALT = "access-salt";
    public final static String R_SALT = "refresh-salt";
    public final static String COMBINED_TOKEN = "combined-token";
    public final static String COMBINED_SALT = "combined-salt";

    private final static Logger logger = LoggerFactory.getLogger(DiscordOAuthHelper.class);

    @Deprecated
    public static Optional<KMTokens> getTokens(Request req) throws Exception {
        String accessToken = req.cookie(A_TOKEN);
        String refreshToken = req.cookie(R_TOKEN);
        String aSalt = req.cookie(A_SALT);
        String rSalt = req.cookie(R_SALT);
        req.attribute("areTokensNew", false);

        boolean noSalts = (aSalt == null || aSalt == "") || (rSalt == null || rSalt == "");
        if (accessToken == null && refreshToken == null || noSalts) {
            return Optional.empty();
        } else if (accessToken == null && refreshToken != null) {
            try {
                refreshToken = CryptoHelper.decrypt(refreshToken, rSalt);
                TokensResponse tokensResponse = DiscordOAuthHelper.getOAuth().refreshTokens(refreshToken);
                req.attribute("areTokensNew", true);
                return Optional.of(KMTokens.generate(tokensResponse));
            } catch (IOException ioException) {
                logger.info("Failed to refresh access token!", ioException);
                return Optional.empty();
            }
        } else if (accessToken != null && refreshToken == null) {
            return Optional.empty();
        } else {
            boolean doRefresh = false;

            accessToken = CryptoHelper.decrypt(accessToken, aSalt);
            refreshToken = CryptoHelper.decrypt(refreshToken, rSalt);

            try {
                DiscordAPI discordAPI = new DiscordAPI(accessToken);
                discordAPI.fetchUser();
            } catch (IOException ioException) {
                doRefresh = true;
            }

            if (doRefresh == false) {
                return Optional.of(new KMTokens(accessToken, refreshToken));
            }

            try {
                req.attribute("areTokensNew", true);
                return Optional.of(
                        KMTokens.generate(
                                DiscordOAuthHelper.getOAuth().refreshTokens(refreshToken)));
            } catch (IOException ioException) {
                return Optional.empty();
            }
        }
    }

    public static Optional<KMTokens> getCombinedTokens(Request req) throws Exception {
        String combinedToken = req.cookie(DiscordOAuthFilter.COMBINED_TOKEN);
        String combinedSalt = req.cookie(DiscordOAuthFilter.COMBINED_SALT);
        req.attribute("areTokensNew", false);

        if (combinedToken == null || combinedToken.isEmpty() || combinedSalt == null || combinedSalt.isEmpty()) {
            return Optional.empty();
        }

        String decryptedTokens;
        try {
            decryptedTokens = CryptoHelper.decrypt(combinedToken, combinedSalt);
        } catch (Exception e) {
            logger.info("Failed to decrypt combined token!", e);
            return Optional.empty();
        }

        String[] tokens = decryptedTokens.split(":");
        if (tokens.length != 2) {
            logger.info("Invalid token format after decryption.");
            return Optional.empty();
        }

        String accessToken = tokens[0];
        String refreshToken = tokens[1];

        boolean doRefresh = false;

        try {
            DiscordAPI discordAPI = new DiscordAPI(accessToken);
            discordAPI.fetchUser();
        } catch (IOException ioException) {
            doRefresh = true;
        }

        if (!doRefresh) {
            return Optional.of(new KMTokens(accessToken, refreshToken));
        }

        try {
            refreshToken = CryptoHelper.decrypt(refreshToken, combinedSalt);
            TokensResponse tokensResponse = DiscordOAuthHelper.getOAuth().refreshTokens(refreshToken);
            req.attribute("areTokensNew", true);
            return Optional.of(KMTokens.generate(tokensResponse));
        } catch (IOException ioException) {
            logger.info("Failed to refresh access token!", ioException);
            return Optional.empty();
        }
    }

    public static Optional<KMTokens> getCombinedTokens(String combinedToken, String combinedSalt) throws Exception {
        if (combinedToken == null || combinedToken.isEmpty() || combinedSalt == null || combinedSalt.isEmpty()) {
            return Optional.empty();
        }

        String decryptedTokens;
        try {
            decryptedTokens = CryptoHelper.decrypt(combinedToken, combinedSalt);
        } catch (Exception e) {
            logger.info("Failed to decrypt combined token!", e);
            return Optional.empty();
        }

        String[] tokens = decryptedTokens.split(":");
        if (tokens.length != 2) {
            logger.info("Invalid token format after decryption.");
            return Optional.empty();
        }

        String accessToken = tokens[0];
        String refreshToken = tokens[1];

        boolean doRefresh = false;

        try {
            DiscordAPI discordAPI = new DiscordAPI(accessToken);
            discordAPI.fetchUser();
        } catch (IOException ioException) {
            doRefresh = true;
        }

        if (!doRefresh) {
            return Optional.of(new KMTokens(accessToken, refreshToken));
        }

        try {
            refreshToken = CryptoHelper.decrypt(refreshToken, combinedSalt);
            TokensResponse tokensResponse = DiscordOAuthHelper.getOAuth().refreshTokens(refreshToken);
            return Optional.of(KMTokens.generate(tokensResponse));
        } catch (IOException ioException) {
            logger.info("Failed to refresh access token!", ioException);
            return Optional.empty();
        }
    }
}
