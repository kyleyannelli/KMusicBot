package dev.kmfg.musicbot.core.websocket;

import com.google.gson.annotations.Expose;

/**
 * Non-conventional error messages describing the exact behavior occurring in
 * the bot.
 * Mainly intended for the ReadonlyClient to understand what is going on if
 * needed.
 */
public enum WebsocketMessages {
    ERROR(500, "An error occurred on the server."),
    NOT_AUTHENTICATED(401, "You failed to provide the proper authentication."),
    NOT_READY(425, "The bot is not ready!"),
    MAX_CONNECTIONS(503, "Websocket server has reached max open connections."),
    GUILD_UNAVAILABLE(404, "The requested guild is not available, doesn't exist, or ID was formatted incorrectly."),
    NOTHING_PLAYING(204, "The requested guild does not have a bot in a music channel.");

    @Expose
    private final int code;
    @Expose
    private final String reason;

    WebsocketMessages(int code, String reason) {
        this.reason = reason;
        this.code = code;
    }

    @Override
    public String toString() {
        return this.reason;
    }

    public int getCode() {
        return this.code;
    }

    public WebsocketResponse toResponse() {
        return new WebsocketResponse(code, reason);
    }
}
