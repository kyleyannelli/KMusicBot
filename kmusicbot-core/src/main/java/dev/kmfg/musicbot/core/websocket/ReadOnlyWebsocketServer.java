package dev.kmfg.musicbot.core.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.tinylog.Logger;

import dev.kmfg.musicbot.core.KMusicBot;
import dev.kmfg.musicbot.core.sessions.AudioSession;
import io.github.cdimascio.dotenv.Dotenv;

import java.lang.reflect.Type;

public class ReadOnlyWebsocketServer extends WebSocketServer {
    private static class SafeTypeAdapter implements JsonSerializer<Long> {
        @Override
        public JsonElement serialize(Long src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Long.class, new SafeTypeAdapter())
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    private final KMusicBot bot;
    private final Dotenv dotenv;
    private final String WEBSOCKET_AUTH_HEADER;
    private final int MAX_CONNECTION_COUNT = 1;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(MAX_CONNECTION_COUNT);
    private final ConcurrentHashMap<WebSocket, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    private final AtomicInteger connectionCount = new AtomicInteger(0);

    public ReadOnlyWebsocketServer(KMusicBot bot, InetSocketAddress address) throws Exception {
        super(address);

        this.bot = bot;
        this.dotenv = Dotenv.load();

        final String TOKEN_TYPE = "Bearer ";
        this.WEBSOCKET_AUTH_HEADER = TOKEN_TYPE + this.dotenv.get("WEBSOCKET_TOKEN");
        if (WEBSOCKET_AUTH_HEADER.length() <= TOKEN_TYPE.length()) {
            throw new Exception("Websocket password is empty. Cannot start websocket!");
        }
    }

    @Override
    public void onClose(WebSocket ws, int arg1, String arg2, boolean arg3) {
        stopScheduledTask(ws);
        this.connectionCount.set(Math.max(0, this.connectionCount.get() - 1));
    }

    @Override
    public void onError(WebSocket ws, Exception e) {
        Logger.error(e, "Error in bot websocket!");
        if (ws != null) {
            ws.send(gson.toJson(WebsocketMessages.ERROR.toResponse()));
        }
    }

    @Override
    public void onMessage(WebSocket ws, String message) {
        handleMessage(ws, message);
    }

    @Override
    public void onOpen(WebSocket ws, ClientHandshake ch) {
        if (this.bot.getSessionManager() == null) {
            ws.send(gson.toJson(WebsocketMessages.NOT_READY.toResponse()));
            ws.close();
            return;
        }

        if (connectionCount.incrementAndGet() > MAX_CONNECTION_COUNT) {
            ws.send(gson.toJson(WebsocketMessages.MAX_CONNECTIONS.toResponse()));
            ws.close();
            return;
        }

        String authHeader = ch.getFieldValue("Authorization");
        if (!isValidAuth(authHeader)) {
            ws.send(gson.toJson(WebsocketMessages.NOT_AUTHENTICATED.toResponse()));
            ws.close();
            return;
        }

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            if (ws.isOpen()) {
                sendAllGuildsNowPlaying(ws);
            } else {
                stopScheduledTask(ws);
            }
        }, 0, 250, TimeUnit.MILLISECONDS);
        scheduledTasks.put(ws, task);
    }

    @Override
    public void onStart() {
        Logger.info("Bot websocket server started!");
    }

    private void stopScheduledTask(WebSocket ws) {
        ScheduledFuture<?> task = scheduledTasks.remove(ws);
        if (task != null) {
            task.cancel(true);
        }
    }

    private void sendAllGuildsNowPlaying(WebSocket ws) {
        ArrayList<NowPlayingResponse> nowPlayings = new ArrayList<>();
        for (Long guildId : this.bot.getSessionManager().getAllAudioSessions().keySet()) {
            nowPlayings.add(new NowPlayingResponse(guildId, this.bot.getSessionManager().getAudioSession(guildId)));
        }
        WebsocketResponse websocketResponse = new WebsocketResponse(200,
                nowPlayings);
        ws.send(gson.toJson(websocketResponse));
    }

    private void handleMessage(WebSocket ws, String message) {
        long guildId = -1;

        try {
            guildId = Long.valueOf(message);
        } catch (NumberFormatException nfe) {
            Logger.warn(String.format("Could not parse requested guild ID \"%s\" in websocket.", message));
        }

        if (guildId == -1) {
            ws.send(gson.toJson(WebsocketMessages.GUILD_UNAVAILABLE.toResponse()));
            return;
        }

        AudioSession audioSession = this.bot.getSessionManager().getAudioSession(guildId);
        if (audioSession == null) {
            ws.send(gson.toJson(WebsocketMessages.NOTHING_PLAYING.toResponse()));
            return;
        }

        WebsocketResponse websocketResponse = new WebsocketResponse(200,
                gson.toJson(new NowPlayingResponse(guildId, audioSession)));
        ws.send(gson.toJson(websocketResponse));
    }

    private boolean isValidAuth(String authHeader) {
        return authHeader.equals(WEBSOCKET_AUTH_HEADER);
    }
}
