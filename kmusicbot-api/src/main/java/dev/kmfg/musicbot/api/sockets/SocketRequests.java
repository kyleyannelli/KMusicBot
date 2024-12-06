package dev.kmfg.musicbot.api.sockets;

public enum SocketRequests {
    NOT_AVAILABLE("Socket already has a connection."),
    TOO_LONG_NO_INFO("Serialized KMusicBot has not been received quickly enough."),
    SENDING_BOT("Serialized KMusicBot has been sent."),
    BOT_SHUTDOWN("The bot has shutdown."),
    STALE_CONN("Connection has become stale due to issues with KMusicBot object or the session."),
    CLEANUP("Socket closing due to cleanup. Likely intended."),
    SESSION_EXPIRED("The current session has exceeded the allowed connection time. Please reconnect."),
    CREATE_BOT("Session has begun. Please provide serialized KMusicBot.");

    private final String reason;

    SocketRequests(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return this.reason;
    }
}
