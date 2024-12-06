package dev.kmfg.musicbot.api;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.tinylog.Logger;

import dev.kmfg.musicbot.api.controllers.HealthCheckController;
import dev.kmfg.musicbot.api.routes.ApiV1;
import dev.kmfg.musicbot.api.sockets.NowPlayingResponse;
import dev.kmfg.musicbot.api.sockets.ReadOnlyWebsocketClient;
import io.github.cdimascio.dotenv.Dotenv;
import spark.Spark;

public class RunApi {
    private static final int MAX_API_THREADS = 15;
    private static final int API_PORT = 8712;
    private static final int RECONNECT_INTERVAL_SECONDS = 5;
    private static final ConcurrentHashMap<Long, NowPlayingResponse> guildsNowPlaying = new ConcurrentHashMap<>();

    private static ReadOnlyWebsocketClient wsClient;
    private static URI websocketUri;
    private static Map<String, String> headers;
    private static ScheduledExecutorService scheduler;

    public static void main(String[] args) throws Exception {
        Spark.port(API_PORT);
        Spark.threadPool(MAX_API_THREADS);

        Dotenv dotenv = Dotenv.load();
        String websocketToken = dotenv.get("WEBSOCKET_TOKEN");
        websocketUri = new URI(dotenv.get("WEBSOCKET_URI").replace("\"", "").trim());

        headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + websocketToken);

        try {
            runWebsocketClient();
            startReconnectScheduler();
        } catch (Exception e) {
            Logger.error(e, "Couldn't initialize the websocket client or scheduler!");
        }

        new ApiV1(new HealthCheckController(), guildsNowPlaying);
    }

    private static void runWebsocketClient() {
        wsClient = new ReadOnlyWebsocketClient(websocketUri, headers, guildsNowPlaying);
        Thread wsClientThread = new Thread(() -> {
            try {
                wsClient.connectBlocking();
            } catch (InterruptedException e) {
                Logger.error(e, "WebSocket client connection was interrupted.");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Logger.error(e, "Failed to start WebSocket client.");
            }
        });
        wsClientThread.setDaemon(true);
        startThread(wsClientThread);
    }

    private static void startThread(Thread thread) {
        try {
            thread.start();
            Logger.info("Started websocket client thread!");
        } catch (Exception e) {
            Logger.error(e, "Couldn't start the websocket client thread!");
        }
    }

    private static void startReconnectScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (wsClient != null && !wsClient.isOpen()) {
                Logger.warn("WebSocket client disconnected. Attempting to reconnect...");
                try {
                    wsClient.closeBlocking();
                } catch (InterruptedException e) {
                    Logger.error(e, "Error closing WebSocket client during reconnection.");
                    Thread.currentThread().interrupt(); // Restore interrupted status
                }

                runWebsocketClient();
            }
        }, RECONNECT_INTERVAL_SECONDS, RECONNECT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }
}
