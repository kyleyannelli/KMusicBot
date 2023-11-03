package dev.kmfg.musicbot.core.lavaplayer;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import dev.kmfg.musicbot.database.models.DiscordUser;

public class AudioTrackWithUser {
    private final AudioTrack audioTrack;
    private final DiscordUser discordUser;

    public AudioTrackWithUser(AudioTrack audioTrack, DiscordUser discordUser) {
        this.audioTrack = audioTrack;
        this.discordUser = discordUser;
    }

    public AudioTrack getAudioTrack() {
        return this.audioTrack;
    }

    public DiscordUser getDiscordUser() {
        return this.discordUser;
    }
}

