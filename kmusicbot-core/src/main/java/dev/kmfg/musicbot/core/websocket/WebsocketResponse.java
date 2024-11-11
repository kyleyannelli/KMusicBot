package dev.kmfg.musicbot.core.websocket;

import java.util.List;

import com.google.gson.annotations.Expose;

public class WebsocketResponse {
    @Expose
    private final int code;

    @Expose
    private final String message;

    @Expose
    private final List<NowPlayingResponse> data;

    public WebsocketResponse(int code, List<NowPlayingResponse> data) {
        this.code = code;
        this.message = null;
        this.data = data;
    }

    public WebsocketResponse(int code, String message) {
        this.code = code;
        this.message = message;
        this.data = null;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
