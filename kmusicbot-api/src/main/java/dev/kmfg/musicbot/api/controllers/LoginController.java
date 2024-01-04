package dev.kmfg.musicbot.api.controllers;

import java.io.IOException;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import balbucio.discordoauth.DiscordAPI;
import balbucio.discordoauth.model.TokensResponse;
import balbucio.discordoauth.model.User;
import dev.kmfg.musicbot.api.helpers.DiscordOAuthHelper;
import spark.Request;
import spark.Response;

public class LoginController {
    private final static Logger logger = LoggerFactory.getLogger(LoginController.class);
    private final static String A_TOKEN = "access-token";
    private final static String R_TOKEN = "refresh-token";

    public static String callback(Request req, Response res) throws IOException {
        TokensResponse tokens;
        String code = req.queryParamOrDefault("code", "");
        String state = req.queryParamOrDefault("state", "");
        String accessToken = req.cookie(A_TOKEN);
        String refreshToken = req.cookie(R_TOKEN);
        if(code == "" && accessToken == null && refreshToken == null) {
            res.status(400);
            return "Bad Request! Missing code parameter...";
        }
        else if(code == "" && accessToken != null && refreshToken != null) {
            if(state != "") res.redirect(state);
            return "Logged in...";
        }
        else {
            tokens = DiscordOAuthHelper.getOAuth().getTokens(code);
            DiscordOAuthHelper.setupCookies(res, tokens);
            if(state != "") res.redirect(state);
            return "Logged in...";
        }
    }

    public static String login(Request req, Response res, String returnUrl) {
        String redirectUrl = DiscordOAuthHelper.generateAuthUrl(returnUrl)[0];
        res.redirect(redirectUrl);
        return "Redirecting...";
    }

    public static String login(Request req, Response res) {
        String redirectUrl = DiscordOAuthHelper.generateAuthUrl()[0];
        res.redirect(redirectUrl);
        return "Redirecting...";
    }

    public static String logout(Request req, Response res) {
        for(String k : req.cookies().keySet()) {
            logger.debug("Removing cookie \"" + k + "\"");
            res.cookie("/", k, "", 0, false, true);
            res.removeCookie(k);
        }
        return "Logged out!";
    }

    public static String getMe(Request req, Response res) {
        DiscordAPI discordAPI = new DiscordAPI(req.cookie(A_TOKEN));
        try {
            User usa = discordAPI.fetchUser();
            return usa.toString();
        }
        catch(IOException ioe) {
            res.status(500);
            return "Internal Server Error";
        }
    }
}
