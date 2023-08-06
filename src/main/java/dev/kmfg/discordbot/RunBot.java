package dev.kmfg.discordbot;

import dev.kmfg.KMusicBot;

public class RunBot {
    public static void main(String[] args) {
        // initialize the KMusicBot object.
        KMusicBot kMusicBot = new KMusicBot();
        // Starting the bot will take care of everything as long as the .env has the discord bot token!
        kMusicBot.start();
    }
}
