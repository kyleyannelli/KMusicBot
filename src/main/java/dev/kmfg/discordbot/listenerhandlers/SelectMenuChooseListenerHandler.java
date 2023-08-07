package dev.kmfg.discordbot.listenerhandlers;

import dev.kmfg.helpers.messages.EmbedMessage;
import dev.kmfg.helpers.messages.MessageSender;
import dev.kmfg.helpers.QueueResult;
import dev.kmfg.sessions.AudioSession;
import dev.kmfg.sessions.SessionManager;
import org.javacord.api.entity.message.component.SelectMenuOption;
import org.javacord.api.event.interaction.SelectMenuChooseEvent;
import org.javacord.api.interaction.SelectMenuInteraction;
import org.javacord.api.listener.interaction.SelectMenuChooseListener;
import org.tinylog.Logger;

import java.util.List;

/**
 * Handles the event when a user select the /search menu option
 * Logically equivalent to PlayCommand in terms of how audio is spawned from this handler
 */
public class SelectMenuChooseListenerHandler implements SelectMenuChooseListener {
    private final SessionManager sessionManager;

    public SelectMenuChooseListenerHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * On menu choose, queue the search query. In this case the youtube URI from the Menu Option.
     * @param event The event.
     */
    @Override
    public void onSelectMenuChoose(SelectMenuChooseEvent event) {
        SelectMenuInteraction selectMenuInteraction = event.getSelectMenuInteraction();
        MessageSender messageSender = setupMessageSender(selectMenuInteraction);
        long audioSessionId = this.parseString(selectMenuInteraction.getCustomId());
        if(audioSessionId == -1) {
            messageSender.sendTooOldEmbed();
            return;
        }
        AudioSession audioSession = this.sessionManager.getAudioSession(audioSessionId);
        if(audioSession == null) {
            messageSender.sendTooOldEmbed();
            return;
        }

        // get the first result
        List<SelectMenuOption> chosenOptions = selectMenuInteraction.getChosenOptions();
        if(chosenOptions == null || chosenOptions.isEmpty()) {
            messageSender.sendTooOldEmbed();
            return;
        }

        String firstYoutubeUri = chosenOptions.get(0).getValue();
        QueueResult queueResult = audioSession.queueSearchQuery(firstYoutubeUri);
        messageSender.sendQueueResultEmbed(queueResult);
    }

    /**
     * Sets up the MessageSender
     * @return MessageSender KMusic preferred object to send messages through
     */
    private MessageSender setupMessageSender(SelectMenuInteraction selectMenuInteraction) {
        EmbedMessage embedMessage = new EmbedMessage(selectMenuInteraction, selectMenuInteraction.respondLater());
        return new MessageSender(embedMessage);
    }

    /**
     * Parses the string as a long
     * @return long of the string, -1 if an exception occurs
     */
    private long parseString(String longValue) {
        try {
            return Long.parseLong(longValue);
        }
        catch(NumberFormatException numberFormatException) {
            Logger.error(numberFormatException, "NumberFormatException occurred while attempting to convert menu option id.");
            return -1;
        }
    }
}
