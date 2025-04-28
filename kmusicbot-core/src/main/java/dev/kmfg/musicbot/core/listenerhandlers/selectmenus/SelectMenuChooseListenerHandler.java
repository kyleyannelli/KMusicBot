package dev.kmfg.musicbot.core.listenerhandlers.selectmenus;

import dev.kmfg.musicbot.core.sessions.AudioSession;
import dev.kmfg.musicbot.core.sessions.SessionManager;
import dev.kmfg.musicbot.database.models.DiscordUser;
import dev.kmfg.musicbot.core.util.messages.EmbedMessage;
import dev.kmfg.musicbot.core.util.messages.MessageSender;
import dev.kmfg.musicbot.core.util.sessions.QueueResult;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.SelectMenu;
import org.javacord.api.entity.message.component.SelectMenuOption;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SelectMenuChooseEvent;
import org.javacord.api.interaction.SelectMenuInteraction;
import org.javacord.api.interaction.callback.ComponentInteractionOriginalMessageUpdater;
import org.javacord.api.listener.interaction.SelectMenuChooseListener;
import org.tinylog.Logger;

import java.util.List;

/**
 * Handles the event when a user select the /search menu option
 * Logically equivalent to PlayCommand in terms of how audio is spawned from
 * this handler
 */
public class SelectMenuChooseListenerHandler implements SelectMenuChooseListener {
    private final SessionManager sessionManager;

    public SelectMenuChooseListenerHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * On menu choose, queue the search query. In this case the youtube URI from the
     * Menu Option.
     * 
     * @param event The event.
     */
    @Override
    public void onSelectMenuChoose(SelectMenuChooseEvent event) {
        SelectMenuInteraction selectMenuInteraction = event.getSelectMenuInteraction();

        MessageSender messageSender = this.setupMessageSender(selectMenuInteraction);
        final String idStr = selectMenuInteraction.getChosenOptions()
                .stream()
                .map(SelectMenuOption::getValue)
                .findFirst()
                .orElseGet(() -> "");

        final String[] idSplit = idStr.split(ActionType.SEPARATOR);
        // handle classic song search
        if(idSplit.length == 1) {
            handleSongSearch(selectMenuInteraction.getCustomId(), messageSender, selectMenuInteraction, event);
            return;
        }

        if(idSplit[0].length() != 1) {
            Logger.error("Expected an ActionType ID but received \"%s\"", idSplit[0]);
            messageSender.sendInternalError();
            return;
        }

        if(selectMenuInteraction.getServer().isEmpty()) {
            messageSender.sendInternalError();
            return;
        }

        final long guildId = selectMenuInteraction.getServer().get().getId();
        final char actionTypeId = idSplit[0].charAt(0);
        final ActionType actionType = ActionType.fromChar(actionTypeId);

        switch (actionType) {
            default:
                Logger.error("Could not resolve ActionType ID \"%s\"", idSplit[0]);
                messageSender.sendInternalError();
                break;
            case ADD_TO_PLAYLIST:
                final String playlistName = idSplit[1];
                final String youtubeUrl = idSplit[2];
                AddToPlaylistAction.addLinkToPlaylist(messageSender, guildId, playlistName, youtubeUrl);
                break;
        }
    }

    private void handleSongSearch(
            String idStr,
            MessageSender messageSender,
            SelectMenuInteraction selectMenuInteraction,
            SelectMenuChooseEvent event
    ) {
        long audioSessionId = this.parseString(idStr);
        if (audioSessionId == -1) {
            messageSender.sendTooOldEmbed();
            return;
        }
        AudioSession audioSession = this.sessionManager.getAudioSession(audioSessionId);
        if (audioSession == null) {
            messageSender.sendTooOldEmbed();
            return;
        }

        // get the first result
        List<SelectMenuOption> chosenOptions = selectMenuInteraction.getChosenOptions();
        if (chosenOptions == null || chosenOptions.isEmpty()) {
            messageSender.sendTooOldEmbed();
            return;
        }

        String firstYoutubeUri = chosenOptions.get(0).getValue();
        User user = event.getInteraction().getUser();
        DiscordUser discordUser = new DiscordUser(user.getId(), user.getDiscriminatedName());
        QueueResult queueResult = audioSession.queueSearchQuery(discordUser, firstYoutubeUri);
        messageSender.sendQueueResultEmbed(queueResult);
        audioSession.setIsRecommending(true);
    }

    /**
     * Disables the select menu from interaction and displays the chosen option
     * 
     * @param selectMenuInteraction the interaction
     */
    private void disableSelectMenu(SelectMenuInteraction selectMenuInteraction) {
        ComponentInteractionOriginalMessageUpdater originalMessageUpdater = selectMenuInteraction
                .createOriginalMessageUpdater();
        List<SelectMenuOption> chosenOptions = selectMenuInteraction.getChosenOptions();
        // if there weren't any chosen options, dont bother with the update.
        if (chosenOptions.isEmpty())
            return;
        // get the first for display
        SelectMenuOption optionToDisplay = chosenOptions.get(0);
        // use -1 as the id just so it cant be invoked even by a bypass, use true to
        // disable the menu
        SelectMenu selectMenu = SelectMenu.createStringMenu("-1", List.of(optionToDisplay), true);
        ActionRow actionRow = ActionRow.of(selectMenu);
        originalMessageUpdater.addComponents(actionRow);
        originalMessageUpdater.update();
    }

    /**
     * Sets up the MessageSender
     * 
     * @return MessageSender KMusic preferred object to send messages through
     */
    private MessageSender setupMessageSender(SelectMenuInteraction selectMenuInteraction) {
        EmbedMessage embedMessage = new EmbedMessage(selectMenuInteraction.getUser(),
                selectMenuInteraction.createOriginalMessageUpdater());
        return new MessageSender(embedMessage);
    }

    /**
     * Parses the string as a long
     * 
     * @return long of the string, -1 if an exception occurs
     */
    private long parseString(String longValue) {
        try {
            return Long.parseLong(longValue);
        } catch (NumberFormatException numberFormatException) {
            Logger.error(numberFormatException,
                    "NumberFormatException occurred while attempting to convert menu option id.");
            return -1;
        }
    }
}
