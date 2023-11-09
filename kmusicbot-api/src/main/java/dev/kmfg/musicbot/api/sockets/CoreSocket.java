package dev.kmfg.musicbot.api.sockets;

import dev.kmfg.musicbot.database.models.KMusicBot;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * For communicating with the core discord bot.
 * Only one Session is allowed.
 */
@WebSocket
public class CoreSocket {
    private final int MAX_THREADS = 10;
    private final int MAX_BOT_INFO_AWAIT_SECONDS = 10;
    private final int AUTO_CLEAN_MINS = 1;
    private final ScheduledExecutorService timeoutService;
    private Session onlySession;
    private KMusicBot onlyBot;

    public CoreSocket() {
        this.timeoutService = Executors.newScheduledThreadPool(MAX_THREADS);
        this.timeoutService.scheduleAtFixedRate(this::scheduledCleanup, this.AUTO_CLEAN_MINS, this.AUTO_CLEAN_MINS, TimeUnit.MINUTES);
    }

    @OnWebSocketConnect
    public void connected(Session session) {
        // only one session is allowed "fail" if one exists
        if(this.onlySession != null) {
            session.close(StatusCode.TRY_AGAIN_LATER, SocketRequests.NOT_AVAILABLE.toString());
        }
        else {
            // setup the current session
            this.onlySession = session;
            this.onlySession.getRemote()
                    .sendStringByFuture(SocketRequests.CREATE_BOT.toString());
            // if we don't get a response from the connection regarding the bot status, close the connection.
            this.watchForBot(this.onlySession);
        }
    }

    @OnWebSocketClose
    public void disconnected(Session session, int statusCode, String reason) {
        // this.cleanup();
    }

    @OnWebSocketMessage
    public void messageReceived(Session session, String message) {
        Optional<SocketRequests> reqReason = this.getRequest(message);
        if(reqReason.isEmpty()) {
            session.getRemote().sendStringByFuture("Bad request! \"" + message + "\" is not a valid action.");
            return;
        }
        switch(reqReason.get()) {
            case SENDING_BOT:
                this.onlyBot = new KMusicBot(123L);
                session.getRemote().sendStringByFuture(
                        new StringBuilder()
                                .append("Created ")
                                .append(this.onlyBot)
                                .toString()
                );
                break;
            case BOT_SHUTDOWN:
                session.getRemote().sendStringByFuture(
                        new StringBuilder()
                                .append("Removing ")
                                .append(this.onlyBot)
                                .toString()
                );
                this.onlyBot = null;
                break;
        }
    }

    private Optional<SocketRequests> getRequest(String message) {
        try {
            return Optional.of(SocketRequests.valueOf(message));
        }
        catch(NoSuchElementException | IllegalArgumentException e) {
            Logger.getGlobal().log(Level.WARNING, "No Request with value " + message);
            return Optional.empty();
        }
    }

    private void watchForBot(Session session) {
        this.timeoutService.schedule(() -> this.killSessionIfNoBot(session),
                this.MAX_BOT_INFO_AWAIT_SECONDS, TimeUnit.SECONDS);
    }

    private void killSessionIfNoBot(Session session) {
        if(!this.onlySession.equals(session)) {
            return;
        }
        if(this.onlyBot == null) {
            this.onlySession.close(StatusCode.POLICY_VIOLATION, SocketRequests.TOO_LONG_NO_INFO.toString());
            this.cleanup(false);
        }
    }

    private void scheduledCleanup() {
        if(this.onlySession == null ^ this.onlyBot == null) {
            Logger.getGlobal().log(Level.INFO, "Automatic cleanup fired.");
            this.cleanup(true);
        }
    }

    private void cleanup(boolean isCleaner) {
        if(isCleaner && this.onlySession != null) {
            this.onlySession.close(StatusCode.SERVICE_RESTART, SocketRequests.STALE_CONN.toString());
        }
        this.onlySession = null;
        this.onlyBot = null;
    }
}
