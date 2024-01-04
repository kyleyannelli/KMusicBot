package dev.kmfg.musicbot.api.helpers;

import balbucio.discordoauth.model.TokensResponse;

public class KMTokens {
    private final String ACCESS_TOKEN;
    private final String REFRESH_TOKEN;

    public KMTokens(String aToken, String rToken) {
        this.ACCESS_TOKEN = aToken;
        this.REFRESH_TOKEN = rToken;
    }

    public String getAccessToken() {
        return this.ACCESS_TOKEN;
    }

    public String getRefreshToken() {
        return this.REFRESH_TOKEN;
    }

    public static KMTokens generate(TokensResponse tokensResponse) {
        return new KMTokens(tokensResponse.getAccessToken(), tokensResponse.getRefreshToken());
    }
}
