package dev.kmfg.musicbot.core.listenerhandlers;

import dev.kmfg.musicbot.core.sessions.AudioSession;
import dev.kmfg.musicbot.core.sessions.SessionManager;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberLeaveEvent;
import org.javacord.api.listener.channel.server.voice.ServerVoiceChannelMemberLeaveListener;
import org.tinylog.Logger;

/**
 * Implements the onServerVoiceChannelMemberLeave method to handle what the bot should do on leave events.
 */
public class UserLeaveVoiceListenerHandler implements ServerVoiceChannelMemberLeaveListener {
    protected final SessionManager sessionManager;

    public UserLeaveVoiceListenerHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * If the bot is in the server voice channel where a member has left, it will check to see if it should leave.
     * In the case that there are no longer any actual users (bots do not count), the bot will leave and destroy the Session.
     * @param event The event
     */
    @Override
    public void onServerVoiceChannelMemberLeave(ServerVoiceChannelMemberLeaveEvent event) {
        // make sure the bot is connected to the voice channel
        boolean isBotInServerVoiceChannel = event.getChannel().isConnected(event.getApi().getYourself());

        // doing an early return here as I just don't like to nest since it is less readable.
        boolean isAllBots;
        boolean isEmptyServerVoiceChannel;
        if(isBotInServerVoiceChannel) {
            // get all the connected users to check if they are all bots
            isAllBots = event.getChannel().getConnectedUsers()
                    // get as stream so we can inline with allMatch
                    .stream()
                    // thank you JavaCord for the simple check
                    .allMatch(User::isBot);
            isEmptyServerVoiceChannel = event.getChannel().getConnectedUsers().isEmpty();
        }
        else if(event.getUser().isYourself()) {
            AudioSession session = sessionManager.getAudioSession(event.getServer().getId());
            if(session != null) {
                // make sure to clear tracks before disconnecting
                session.stopAllTracks();
                // get the server id from the event
                long relevantServerId = event.getServer().getId();
                // now disconnect and shutdown the session
                this.disconnectFromEmptyServerVoiceChannel(relevantServerId, event.getChannel());
            }
            return;
        }
        else {
            return;
        }

        AudioSession session = sessionManager.getAudioSession(event.getServer().getId());
        if(session != null) {
            // fire track end event for the user that just left
            session.getLavaSource().fireTrackEndIndividualEvent(event.getUser());
        }
        else {
            Logger.warn("AudioSession is null for server, but bot is connected to voice channel. This is unexpected behavior.");
        }

        // if it is either all bots or the server voice channel is empty, we should disconnect.
        if(isAllBots || isEmptyServerVoiceChannel) {
            // make sure to clear tracks before disconnecting
            session.stopAllTracks();
            // get the server id from the event
            long relevantServerId = event.getServer().getId();
            // now disconnect and shutdown the session
            this.disconnectFromEmptyServerVoiceChannel(relevantServerId, event.getChannel());
        }
    }

    /**
     * Gets the AudioSession connected to the disconnectingServerId.
     * Will warn in logs if AudioSession is null as this is unexpected behavior.
     * @param disconnectingServerId, server id of which to remove the audio session.
     */
    protected void disconnectFromEmptyServerVoiceChannel(long disconnectingServerId, ServerVoiceChannel voiceChannel) {
        // disconnect from the voice channel
        voiceChannel.disconnect();
        // get the audioSession from the manager
        AudioSession audioSession = this.sessionManager.getAudioSession(disconnectingServerId);
        // this shouldn't be null if the bot was connected to a VoiceChannel, but just to be safe.
        if(audioSession != null) {
            // then shutdown the audioSession. The shutdown method calls a SessionCloseHandler which will take care of the cleanup.
            audioSession.shutdown();
        }
        else {
            // otherwise we should log this as this is unexpected behavior
            Logger.warn("A disconnect was attempted from UserLeaveVoiceListenerHandler, but there was no AudioSession for the ServerVoiceChannel! \nThis should NOT happen. \nPlease check the logic of which this function is being used in.");
        }
    }
}
