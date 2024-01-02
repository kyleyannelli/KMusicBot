package dev.kmfg.musicbot.core.listenerhandlers;

import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberJoinEvent;
import org.javacord.api.listener.channel.server.voice.ServerVoiceChannelMemberJoinListener;
import org.tinylog.Logger;

import dev.kmfg.musicbot.core.sessions.AudioSession;
import dev.kmfg.musicbot.core.sessions.SessionManager;

public class UserJoinVoiceListenerHandler implements ServerVoiceChannelMemberJoinListener {
    protected final SessionManager sessionManager;

    public UserJoinVoiceListenerHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

	@Override
	public void onServerVoiceChannelMemberJoin(ServerVoiceChannelMemberJoinEvent memberJoinEvent) {
        ServerVoiceChannel voiceChannel = memberJoinEvent.getChannel();
        
        if(!sessionManager.getDiscordApi().getYourself().isConnected(voiceChannel)) {
            return;
        }

        // fire a track start event for the user that just joined
        AudioSession session = sessionManager.getAudioSession(voiceChannel.getServer().getId());
        
        if(session == null) {
            Logger.warn("AudioSession is null for server, but bot is connected to voice channel. This is unexpected behavior.");
            return;
        }

        session.getLavaSource().fireTrackStartIndividualEvent(memberJoinEvent.getUser());
    }
}
