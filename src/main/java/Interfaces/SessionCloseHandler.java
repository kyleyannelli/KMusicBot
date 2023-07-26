package Interfaces;

@FunctionalInterface
public interface SessionCloseHandler {
	void handle(long associatedServerId);
}
