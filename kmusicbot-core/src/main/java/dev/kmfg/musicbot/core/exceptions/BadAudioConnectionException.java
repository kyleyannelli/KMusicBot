package dev.kmfg.musicbot.core.exceptions;

public class BadAudioConnectionException extends Exception {
    public BadAudioConnectionException() {
        super("The requesting user was either not in a voice channel, or not in the same voice channel as the bot!");
    }
}
