package dev.kmfg.sessions;

@FunctionalInterface
public interface SessionCloseHandler {
	void handle(long associatedServerId);
}
