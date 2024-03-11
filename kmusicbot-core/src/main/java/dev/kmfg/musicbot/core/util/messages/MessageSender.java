package dev.kmfg.musicbot.core.util.messages;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import dev.kmfg.musicbot.core.lavaplayer.AudioTrackWithUser;
import dev.kmfg.musicbot.core.lavaplayer.PositionalAudioTrack;
import dev.kmfg.musicbot.core.util.sessions.QueueResult;

import org.checkerframework.com.google.errorprone.annotations.Var;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.SelectMenu;
import org.javacord.api.entity.message.component.SelectMenuOption;
import org.tinylog.Logger;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MessageSender {
    private final EmbedMessage embedMessage;

    public MessageSender(EmbedMessage embedMessage) {
        this.embedMessage = embedMessage;
    }

    public EmbedMessage getEmbedMessage() {
        return this.embedMessage;
    }

    public void sendNothingPlayingEmbed() {
        this.embedMessage
                .setColor(Color.RED)
                .setTitle("Nothing Playing!")
                .setContent("Nothing was playing, so no action was taken.")
                .send();
    }

    public void sendSeekEmbed(AudioTrack audioTrack, long seekedMs) {
        String hoursMinutesSeconds = convertToHMS(seekedMs);
        String totalHoursMinutesSeconds = convertToHMS(audioTrack.getDuration());

        this.embedMessage
            .setTitle("Seeked.")
            .setColor(Color.BLUE)
            .setContent("Seeked to " + hoursMinutesSeconds + " (HMS) of " + totalHoursMinutesSeconds + " (HMS).")
            .send();
    }

    public String convertToHMS(long milliseconds) {
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(minutes);
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

   public void sendSearchResultEmbed(List<AudioTrack> foundTracks, long serverId) {
        // generate menu options
       List<SelectMenuOption> foundTrackMenuOptions = new ArrayList<>();
       for(AudioTrack audioTrack : foundTracks) {
           String label = audioTrack.getInfo().title + " by " + audioTrack.getInfo().author;
           // make sure label does not exceed length of 100
           label = label.length() > 100 ? label.substring(0, 100) : label;
           SelectMenuOption selectMenuOption = SelectMenuOption
                   // create menu option with [title] by [author]. set the value to the youtube link
                   .create(label, audioTrack.getInfo().uri);
           foundTrackMenuOptions.add(selectMenuOption);
       }
       // now create the menu with the audio session id
       SelectMenu selectMenu = SelectMenu.createStringMenu(String.valueOf(serverId), foundTrackMenuOptions);
       ActionRow actionRow = ActionRow.of(selectMenu);
       this.embedMessage.getRespondLater().thenAccept(acceptance -> {
           acceptance.addComponents(actionRow);
           acceptance.update();
       }).exceptionally(e -> {
           Logger.error(e, "Failed to send Search Result Embed!");
           return null;
       });
   }

    public void sendViewQueueEmbed(ArrayList<PositionalAudioTrack> relevantAudioTracks, int pageNumber, int totalPages) {
        String tracksString = "";

        for(PositionalAudioTrack positionalAudioTrackTrack : relevantAudioTracks) {
            AudioTrack audioTrack = positionalAudioTrackTrack.getAudioTrack();

            String info = audioTrack.getInfo().title + " by " + audioTrack.getInfo().author;
            String uri = audioTrack.getInfo().uri;
            tracksString += positionalAudioTrackTrack.isQueuedByUser() ?
                    positionalAudioTrackTrack.getPosition() + ". [" + info + "]" + "(" + uri + ")\n\n" :
                    "Via AutoQueue: " + "[" + info + "]" + "(" + uri + ")\n\n";
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
                .setContent("Music has stopped. The bot will leave the channel when it's ready.")
                .send();
    }

    public void sendQueueResultEmbed(QueueResult queueResult) {
        if(queueResult == null) {
            this.sendNothingFoundEmbed();
            //early return
            return;
        }
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

    public void sendNowPlayingEmbed(AudioTrack audioTrack) {
        String artistAndTitle = audioTrack.getInfo().title + " by " + audioTrack.getInfo().author;
        // reuse sendQueueResultEmbed
        // force the title to make it fit
        this.embedMessage.setForcedTitle(artistAndTitle);
        long currentPositionMs = audioTrack.getPosition();
        long durationMs = audioTrack.getDuration();
        // force the content to be HH:MM:SS
        this.embedMessage.setForcedContent(convertToHMS(currentPositionMs) + " of " + convertToHMS(durationMs) + " (HH:MM:SS)");
        // mock a queue result so we can use an existing function
        // is success because it is playing, use will play now so it uses the same format as a will play now
        QueueResult mockQueueResult = new QueueResult(true, true, audioTrack);
        // use the existing function
        this.sendQueueResultEmbed(mockQueueResult);
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

    public void sendBadHMS(long ms) {
        this.embedMessage
                .setTitle("Bad Parameter(s)!")
                .setContent("Unable to seek " + (ms * -1000) + " seconds in the song. This is likely longer than the song!")
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

    public void sendTooOldEmbed() {
        this.embedMessage
                .setTitle("Too Old!")
                .setContent("The thing you tried to interact with was too old. You may have taken too long or clicked on an old response.")
                .setColor(Color.BLACK)
                .send();
    }

    public void sendInviteEmbed(String inviteLink) {
        this.embedMessage
                .setTitle("Invite Link")
                .setContent(inviteLink)
                .send();
    }

    public void sendNothingFoundEmbed(String searchQuery) {
        this.embedMessage
                .setTitle("Nothing Found!")
                .setContent("Nothing was found from \"" + searchQuery + "\"")
                .setColor(Color.BLACK)
                .send();
    }

    public void sendNothingFoundEmbed() {
        this.embedMessage
                .setTitle("Nothing Found!")
                .setContent("Nothing was found with your search query. Double check it is a valid direct YouTube link or playlist, and is public.")
                .setColor(Color.BLACK)
                .send();
    }

    public void setForcedTitle(String forcedTitle) {
        this.embedMessage.setForcedTitle(forcedTitle);
    }

    public void sendRemovedEmbed(AudioTrackWithUser audioTrackWithUser) {
        var contentStrBld = new StringBuilder("Originally queued by ")
                .append("<@").append(audioTrackWithUser.getDiscordUser().getDiscordId()).append(">");

        this.embedMessage
                .setForcedContent(contentStrBld.toString())
                .setForcedTitle("Removed.")
                .setForcedField(audioTrackWithUser.getAudioTrack().getInfo().title, audioTrackWithUser.getAudioTrack().getInfo().author)
                .setupAudioTrack(audioTrackWithUser.getAudioTrack())
                .send();
    }
}
