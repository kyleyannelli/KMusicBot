package dev.kmfg.helpers;

import dev.kmfg.discordbot.EmbedMessage;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.awt.*;
import java.util.ArrayList;

public class MessageSender {
    private final EmbedMessage embedMessage;
    public MessageSender(EmbedMessage embedMessage) {
        this.embedMessage = embedMessage;
    }
    public void sendNothingPlayingEmbed() {
        this.embedMessage
                .setColor(Color.RED)
                .setTitle("Nothing Playing!")
                .setContent("Nothing was playing, so no action was taken.")
                .send();
    }

    public void sendViewQueueEmbed(ArrayList<AudioTrack> relevantAudioTracks, int pageNumber, int totalPages) {
        String tracksString = "";

        for(AudioTrack audioTrack : relevantAudioTracks) {
            String info = audioTrack.getInfo().title + " by " + audioTrack.getInfo().author;
            String uri = audioTrack.getInfo().uri;
            tracksString += "[" + info + "]" + "(" + uri + ")\n";
        }

        this.embedMessage
                .setTitle("Page " + pageNumber + " of " + totalPages)
                .setColor(Color.BLUE)
                .setContent(tracksString)
                .send();
    }

    public void sendEmptyQueueEmbed() {
        this.embedMessage
                .setColor(Color.BLACK)
                .setTitle("Empty Queue.")
                .setContent("The queue is empty!")
                .send();
    }

    public void sendOutOfBoundsEmbed(int requestedPage, int totalPages) {
        this.embedMessage
                .setColor(Color.RED)
                .setTitle("Page is Out of Bounds!")
                .setContent("Page #" + requestedPage + " is beyond the " + totalPages + " available pages.")
                .send();
    }

    public void sendStoppedEmbed() {
        this.embedMessage.setColor(Color.BLACK)
                .setTitle("Stopped.")
                .setContent("Music has stopped, and bot has left the channel.")
                .send();
    }

    public void sendQueueResultEmbed(QueueResult queueResult) {
        // check if the track(s) went into the AudioQueue by flipping willPlayNow()
        this.embedMessage.setIsQueue(!queueResult.willPlayNow());

        // if the tracks were successfully queued & the size of the queue is greater than 1, just display the # of tracks added.
        if(queueResult.isSuccess() && queueResult.getQueuedTracks() != null && queueResult.getQueuedTracks().size() > 1) {
            this.embedMessage
                    .setTitle("Queued!")
                    .setContent("Added " + queueResult.getQueuedTracks().size() + " tracks to the queue.");
        }
        // otherwise, if the track added successfully & it was only 1 track, use the setupAudioTrack method to display a single track!
        else if(queueResult.isSuccess() && queueResult.getQueuedTracks() != null && queueResult.getQueuedTracks().size() == 1) {
            this.embedMessage.setupAudioTrack(queueResult.getQueuedTracks().get(0));
        }
        else if(queueResult.isSuccess() && queueResult.getQueueTrack() != null) {
            this.embedMessage.setupAudioTrack(queueResult.getQueueTrack());
        }
        else if(!queueResult.isSuccess()) {
            this.embedMessage
                    .setColor(Color.RED)
                    .setTitle("Oops!")
                    .setContent("There was an issue finding your query!");
        }
        else {
            this.embedMessage
                    .setColor(Color.RED)
                    .setTitle("Oops!")
                    .setContent("There was an uncaught edge case. Please report this to <@806350925723205642>!");
        }

        this.embedMessage.send();
    }

    public void sendNotInServerVoiceChannelEmbed() {
        this.embedMessage
                .setTitle("Denied!")
                .setContent("You are either not in a voice channel, or not in the same voice channel the bot is active in.")
                .setColor(Color.RED)
                .send();
    }

    public void sendEmptyParameterEmbed(String parameter) {
        this.embedMessage
                .setTitle("Missing Parameter(s)!")
                .setContent("Parameter \"" + parameter + "\" was missing! Please include all required parameters in your command and try again.")
                .setColor(Color.RED)
                .send();
    }
    
    public void sendBadParameterEmbed(String parameter) {
        this.embedMessage
                .setTitle("Bad Parameter(s)!")
                .setContent("Parameter \"" + parameter + "\" was not of the expected type. Please double check your command usage.")
                .setColor(Color.RED)
                .send();
    }

    public void sendEmptyServerEmbed() {
        this.embedMessage
                .setTitle("Uh Oh!")
                .setContent("The server was not present in the interaction. This shouldn't happen, but in the case you see this contact <@806350925723205642>.")
                .send();
    }

    public void sendInviteEmbed(String inviteLink) {
        this.embedMessage
                .setTitle("Invite Link")
                .setContent(inviteLink)
                .send();
    }

    public void setForcedTitle(String forcedTitle) {
        this.embedMessage.setForcedTitle(forcedTitle);
    }
}
