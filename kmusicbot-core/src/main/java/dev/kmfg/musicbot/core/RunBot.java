package dev.kmfg.musicbot.core;

import java.net.InetSocketAddress;

import org.tinylog.Logger;

import dev.kmfg.musicbot.core.websocket.ReadOnlyWebsocketServer;

public class RunBot {
    private static final int WEBSOCKET_PORT = 30106;

    public static void main(String[] args) {
        // initialize the KMusicBot object.
        KMusicBot kMusicBot = new KMusicBot();

        // start the websocket server in a new thread.
        // Similar to the database handling, if it fails the bot will run fine anyway.
        startWebSocketServer(kMusicBot);

        // Starting the bot will take care of everything as long as the .env has the
        // discord bot token!
        kMusicBot.start();
    }

    private static void startWebSocketServer(KMusicBot kMusicBot) {
        InetSocketAddress address = new InetSocketAddress(WEBSOCKET_PORT);

        ReadOnlyWebsocketServer websocketServer;
        try {
            websocketServer = new ReadOnlyWebsocketServer(kMusicBot, address);
        } catch (Exception e) {
            Logger.error(e, "Failed to create the bot websocket server!");
            return;
        }

        Thread websocketServerThread = new Thread(() -> {
            try {
                websocketServer.start();
            } catch (Exception e) {
                Logger.error(e, "Couldn't start the websocket server!");
            }
        });

        websocketServerThread.setDaemon(true);
        startThread(websocketServerThread);
    }

    private static void startThread(Thread thread) {
        try {
            thread.start();
            Logger.info("Started bot websocket!");
        } catch (Exception e) {
            Logger.error(e, "Couldn't start the websocket server thread!");
        }
    }
}
