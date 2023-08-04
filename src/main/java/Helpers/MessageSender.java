package Helpers;

import DiscordBot.EmbedMessage;

import java.awt.*;

public class MessageSender {
    private final EmbedMessage embedMessage;
    public MessageSender(EmbedMessage embedMessage) {
        this.embedMessage = embedMessage;
    }
    public void sendNothingPlayingEmbed() {
        this.embedMessage.setColor(Color.RED);
        this.embedMessage.setTitle("Nothing Playing!");
        this.embedMessage.setContent("Nothing was playing, so no action was taken.");
        this.embedMessage.send();
    }

    public void sendStoppedEmbed() {
        this.embedMessage.setColor(Color.BLACK);
        this.embedMessage.setTitle("Stopped.");
        this.embedMessage.setContent("Music has stopped, and bot has left the channel.");
        this.embedMessage.send();
    }

    public void sendQueueResultEmbed(QueueResult queueResult) {
        // check if the track(s) went into the AudioQueue by flipping willPlayNow()
        this.embedMessage.setIsQueue(!queueResult.willPlayNow());

        // if the tracks were successfully queued & the size of the queue is greater than 1, just display the # of tracks added.
        if(queueResult.isSuccess() && queueResult.getQueuedTracks() != null && queueResult.getQueuedTracks().size() > 1) {
            this.embedMessage.setTitle("Queued!");
            this.embedMessage.setContent("Added " + queueResult.getQueuedTracks().size() + " tracks to the queue.");
        }
        // otherwise, if the track added successfully & it was only 1 track, use the setupAudioTrack method to display a single track!
        else if(queueResult.isSuccess() && queueResult.getQueuedTracks() != null && queueResult.getQueuedTracks().size() == 1) {
            this.embedMessage.setupAudioTrack(queueResult.getQueuedTracks().get(0));
        }
        else if(queueResult.isSuccess() && queueResult.getQueueTrack() != null) {
            this.embedMessage.setupAudioTrack(queueResult.getQueueTrack());
        }
        else if(!queueResult.isSuccess()) {
            this.embedMessage.setColor(Color.RED);
            this.embedMessage.setTitle("Oops!");
            this.embedMessage.setContent("There was an issue finding your query!");
        }
        else {
            this.embedMessage.setColor(Color.RED);
            this.embedMessage.setTitle("Oops!");
            this.embedMessage.setContent("There was an uncaught edge case. Please report this to <@806350925723205642>!");
        }

        this.embedMessage.send();
    }

    public void sendNotInServerVoiceChannelMessage() {
        this.embedMessage.setTitle("Denied!");
        this.embedMessage.setContent("You are either not in a voice channel, or not in the same voice channel the bot is active in.");
        this.embedMessage.setColor(Color.RED);
        this.embedMessage.send();
    }

    public void sendEmptyParameterMessage(String parameter) {
        this.embedMessage.setTitle("Missing Parameter(s)!");
        this.embedMessage.setContent("Parameter \"" + parameter + " was missing! Please include all required parameters in your command and try again.");
        this.embedMessage.setColor(Color.RED);
        this.embedMessage.send();
    }

    public void sendEmptyServerMessage() {
        this.embedMessage.setTitle("Uh Oh!");
        this.embedMessage.setContent("The server was not present in the interaction. This shouldn't happen, but in the case you see this contact <@806350925723205642>.");
        this.embedMessage.send();
    }

    public void sendInviteMessage(String inviteLink) {
        this.embedMessage.setTitle("Invite Link");
        this.embedMessage.setContent(inviteLink);
        this.embedMessage.send();
    }

    public void setForcedTitle(String forcedTitle) {
        this.embedMessage.setForcedTitle(forcedTitle);
    }
}
