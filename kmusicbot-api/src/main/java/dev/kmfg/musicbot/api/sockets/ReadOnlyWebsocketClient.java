package dev.kmfg.musicbot.api.sockets;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.tinylog.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ReadOnlyWebsocketClient extends WebSocketClient {
    private final ConcurrentHashMap<Long, NowPlayingResponse> guildsNowPlaying;

    private enum LogLevel {
        WARN,
        INFO,
        ERROR;
    }

    private String lastLogMessage;

    public ReadOnlyWebsocketClient(URI uri, Map<String, String> customHeaders,
            ConcurrentHashMap<Long, NowPlayingResponse> guildsNowPlaying) {
        super(uri, customHeaders);
        this.guildsNowPlaying = guildsNowPlaying;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        this.logOnce(LogLevel.INFO, "Connection with bot websocket server has opened...");
    }

    @Override
    public void onMessage(String message) {
        JsonObject jsonMessage = JsonParser.parseString(message).getAsJsonObject();
        int code = jsonMessage.get("code").getAsInt();

        if (code == 200) {
            JsonArray jsonArray = jsonMessage.get("data").getAsJsonArray();
            for (JsonElement jsonElement : jsonArray) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                storeNowPlayingResponse(jsonObject);
            }
            this.logOnce(LogLevel.INFO, "Parsed Guilds!");
        } else {
            String reason = jsonMessage.get("message").getAsString();
            this.logOnce(LogLevel.ERROR,
                    String.format("Received error code from bot websocket server: %d, Reason: %s", code, reason));
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        this.logOnce(LogLevel.WARN, String.format("Websocket with bot server has closed: %d | %s | %s", code, reason,
                remote ? "remote" : "not remote"));
    }

    @Override
    public void onError(Exception e) {
        this.logOnce(LogLevel.ERROR, "Error in API receiving websocket client!", e);
        if (this.isOpen()) {
            this.close();
        }
    }

    public Optional<NowPlayingResponse> getGuildNowPlaying(long guildId) {
        return Optional.ofNullable(this.guildsNowPlaying.get(guildId));
    }

    private void storeNowPlayingResponse(JsonObject jsonObject) {
        final long guildId = jsonObject.get("guildId").getAsLong();
        if (guildId == -1) {
            return;
        }

        final String youtubeUri = jsonObject.get("youtubeUri").getAsString();
        final String title = jsonObject.get("title").getAsString();
        final String author = jsonObject.get("author").getAsString();
        final long positionMs = jsonObject.get("positionMs").getAsLong();
        final long lengthMs = jsonObject.get("lengthMs").getAsLong();

        this.guildsNowPlaying.put(
                guildId,
                new NowPlayingResponse(guildId, youtubeUri, title, author, positionMs, lengthMs));
    }

    private void logOnce(LogLevel logLevel, String message) {
        this.logOnce(logLevel, message, null);
    }

    private void logOnce(LogLevel logLevel, String message, Exception e) {
        String messageWithException = null;

        if (e != null) {
            messageWithException = String.format("%s: %s", message, e);
        } else {
            messageWithException = message;
        }

        if (messageWithException.equals(lastLogMessage)) {
            return;
        } else {
            lastLogMessage = messageWithException;
        }

        switch (logLevel) {
            default:
                Logger.info(messageWithException);
            case INFO:
                Logger.info(messageWithException);
                break;
            case WARN:
                Logger.warn(messageWithException);
                break;
            case ERROR:
                Logger.error(messageWithException);
                break;
        }
    }
}
