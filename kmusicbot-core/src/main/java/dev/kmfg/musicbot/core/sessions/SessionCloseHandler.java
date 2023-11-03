package dev.kmfg.musicbot.core.sessions;

@FunctionalInterface
public interface SessionCloseHandler {
	void handle(long associatedServerId);
}
