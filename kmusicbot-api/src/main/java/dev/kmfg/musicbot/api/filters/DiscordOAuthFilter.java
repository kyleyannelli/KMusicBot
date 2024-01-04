package dev.kmfg.musicbot.api.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import balbucio.discordoauth.model.TokensResponse;
import balbucio.discordoauth.DiscordAPI;

import dev.kmfg.musicbot.api.helpers.DiscordOAuthHelper;
import dev.kmfg.musicbot.api.helpers.KMTokens;

import spark.Request;

import java.util.Optional;
import java.io.IOException;

public class DiscordOAuthFilter {
    public final static String A_TOKEN = "access-token";
    public final static String R_TOKEN = "refresh-token";
    private final static Logger logger = LoggerFactory.getLogger(DiscordOAuthHelper.class);

    public static Optional<KMTokens> getTokens(Request req) {
        String accessToken = req.cookie(A_TOKEN);
        String refreshToken = req.cookie(R_TOKEN);

        if(accessToken == null && refreshToken == null) {
            return Optional.empty();
        }
        else if(accessToken == null && refreshToken != null) {
            try {
                TokensResponse tokensResponse = DiscordOAuthHelper.getOAuth().refreshTokens(refreshToken);
                return Optional.of(KMTokens.generate(tokensResponse));
            }
            catch(IOException ioException) {
                logger.info("Failed to refresh access token!", ioException);
                return Optional.empty();
            }
        }
        else if(accessToken != null && refreshToken == null) {
            return Optional.empty();
        }
        else {
            boolean doRefresh = false;

            try {
                DiscordAPI discordAPI = new DiscordAPI(accessToken);
                discordAPI.fetchUser();
            }
            catch(IOException ioException) {
                doRefresh = true;
            }

            if(doRefresh == false) {
                return Optional.of(new KMTokens(accessToken, refreshToken));
            }

            try {
                return Optional.of(
                        KMTokens.generate(
                            DiscordOAuthHelper.getOAuth().refreshTokens(refreshToken)
                            ));
            }
            catch(IOException ioException) {
                return Optional.empty();
            }
        }
    }
}
