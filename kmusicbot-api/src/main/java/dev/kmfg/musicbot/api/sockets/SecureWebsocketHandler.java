package dev.kmfg.musicbot.api.sockets;

import java.io.IOException;
import java.net.HttpCookie;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.tinylog.Logger;

import balbucio.discordoauth.model.Guild;
import dev.kmfg.musicbot.api.filters.DiscordOAuthFilter;
import dev.kmfg.musicbot.api.helpers.GenericHelpers;
import dev.kmfg.musicbot.api.helpers.KMTokens;

@WebSocket
public class SecureWebsocketHandler {
    private static final long MAX_SESSION_SECONDS = 60 * 15;

    private final char[] key;
    private final ScheduledThreadPoolExecutor messageSendExecutor;
    private final ConcurrentHashMap<Long, NowPlayingResponse> guildsNowPlaying;
    private final ConcurrentHashMap<String, Session> refreshTokenToSession;
    private final ConcurrentHashMap<Session, ScheduledFuture<?>> sessionToFutures;

    public SecureWebsocketHandler(ConcurrentHashMap<Long, NowPlayingResponse> guildsNowPlaying) {
        this.key = generateRandomKey();

        this.guildsNowPlaying = guildsNowPlaying;
        this.refreshTokenToSession = new ConcurrentHashMap<>();
        this.sessionToFutures = new ConcurrentHashMap<>();

        this.messageSendExecutor = new ScheduledThreadPoolExecutor(0);
        this.messageSendExecutor.setKeepAliveTime(5, TimeUnit.SECONDS);
        this.messageSendExecutor.allowCoreThreadTimeOut(true);
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        UpgradeRequest req = session.getUpgradeRequest();

        String combinedToken = null;
        String combinedSalt = null;
        for (HttpCookie cookie : req.getCookies()) {
            if (combinedSalt != null && combinedToken != null) {
                break;
            } else if (cookie.getName().equals(DiscordOAuthFilter.COMBINED_SALT)) {
                combinedSalt = cookie.getValue();
            } else if (cookie.getName().equals(DiscordOAuthFilter.COMBINED_TOKEN)) {
                combinedToken = cookie.getValue();
            }
        }

        if (combinedSalt == null || combinedToken == null) {
            session.close(401, "Unauthorized!");
            return;
        }

        Optional<KMTokens> kmTokens = Optional.empty();

        try {
            kmTokens = DiscordOAuthFilter.getCombinedTokens(combinedToken, combinedSalt);
        } catch (Exception e) {
            Logger.error(e, "Failed to get combined tokens in websocket!");
            session.close(500, "Internal Server Error");
            return;
        }

        if (kmTokens.isEmpty()) {
            session.close(401, "Unauthorized!");
            return;
        }

        KMTokens tokens = kmTokens.get();
        String refreshToken = obfuscateWithXOR(tokens.getRefreshToken());
        Session previousSession = refreshTokenToSession.get(refreshToken);
        if (previousSession != null) {
            closeAndRemove(refreshToken, String.format("A newer connection was opened for %s.", refreshToken), 1008);
        }

        this.refreshTokenToSession.put(refreshToken, session);
        session.getUpgradeResponse().setHeader("refreshToken", refreshToken);

        Logger.info(String.format("Websocket connection opened for %s", getIdentifiers(req, refreshToken)));

        sendMessages(session, tokens);
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        String refreshToken = session.getUpgradeResponse().getHeader("refreshToken");

        if (refreshToken != null) {
            closeAndRemove(refreshToken);
            Logger.info(
                    String.format("Removed session for %s", getIdentifiers(session.getUpgradeRequest(), refreshToken)));
        }

        Logger.info("Session closed: " + reason + " with code " + statusCode);
    }

    private String getIdentifiers(UpgradeRequest request, String refreshToken) {
        return String.format("%s using %s", request.getHeader("X-Forwarded-For"), refreshToken);
    }

    private void sendMessages(Session session, KMTokens tokens) {
        long createdAt = Instant.now().getEpochSecond();
        ScheduledFuture<?> task = this.messageSendExecutor.scheduleAtFixedRate(() -> {
            if (Instant.now().minusSeconds(createdAt).getEpochSecond() > MAX_SESSION_SECONDS) {
                session.close(1000, SocketRequests.SESSION_EXPIRED.toString());
                return;
            }

            List<Guild> guilds = null;

            try {
                guilds = GenericHelpers.getFromCacheOrAPI(tokens);
            } catch (IOException iE) {
                Logger.warn(iE, "IOE occurred while getting guilds from cache or API in websocket send message!");
                closeAndRemove(tokens);
                return;
            }

            List<NowPlayingResponse> guildsNowPlaying = new ArrayList<>();

            for (Guild guild : guilds) {
                Long guildId = Long.valueOf(guild.getId());
                NowPlayingResponse npr = this.guildsNowPlaying.get(guildId);
                if (npr != null) {
                    guildsNowPlaying.add(npr);
                }
            }

            if (session.isOpen()) {
                try {
                    session.getRemote().sendString(GenericHelpers.provideGson().toJson(guildsNowPlaying));
                } catch (IOException iE) {
                    Logger.warn(iE, "IOE occurred while trying to send string!");
                    session.close(1011, "Internal Server Error");
                    return;
                }
            } else {
                closeAndRemove(tokens);
            }
        }, 0, 1, TimeUnit.SECONDS);
        this.sessionToFutures.put(session, task);
    }

    private void closeAndRemove(String refreshToken, String reason, int code) {
        Session session = this.refreshTokenToSession.remove(refreshToken);

        if (session == null) {
            return;
        }

        ScheduledFuture<?> task = sessionToFutures.remove(session);
        if (task != null) {
            task.cancel(true);
        }

        if (session.isOpen()) {
            session.getUpgradeResponse().setHeader("refreshToken", "-1");
            if (reason != null) {
                session.close(code, reason);
            } else {
                session.close();
            }
        }
    }

    private void closeAndRemove(String refreshToken) {
        closeAndRemove(refreshToken, null, -1);
    }

    private void closeAndRemove(KMTokens kmTokens) {
        closeAndRemove(kmTokens.getRefreshToken());
    }

    public String obfuscateWithXOR(String input) {
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            chars[i] ^= this.key[i % this.key.length];
        }
        return new String(chars);
    }

    private char[] generateRandomKey() {
        SecureRandom secureRandom = new SecureRandom();

        int randomInt = secureRandom.nextInt();

        char[] key = new char[4];
        key[0] = (char) ((randomInt >> 24) & 0xFF);
        key[1] = (char) ((randomInt >> 16) & 0xFF);
        key[2] = (char) ((randomInt >> 8) & 0xFF);
        key[3] = (char) (randomInt & 0xFF);

        return key;
    }
}
