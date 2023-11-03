package dev.kmfg.musicbot.core.exceptions;

public class EmptyServerException extends Exception {
    public EmptyServerException() {
        super("Server was not present in the interaction!");
    }
    public EmptyServerException(String message) {
        super(message);
    }
}
