package Helpers;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import DiscordBot.Commands;
import Exceptions.BadAudioConnectionException;
import Exceptions.EmptyParameterException;
import Exceptions.EmptyServerException;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;

import DiscordBot.AudioSession;
import org.tinylog.Logger;

/**
 * Ensures that the initialization of this object will include,
 * 	1. The discord server object
 * 	2. All required command parameters
 *	3. A valid requested audio session
 */
public class EnsuredSlashCommandInteraction {
	private final Server server;
	private final User user;
	private final ConcurrentHashMap<String, String> parameters;
	private final AudioSession audioSession;

	public EnsuredSlashCommandInteraction(Commands commands, SlashCommandCreateEvent slashCommandCreateEvent, ArrayList<String> requiredParameters) throws EmptyParameterException, BadAudioConnectionException, EmptyServerException {
		// check if the server is empty, this will handle sending a message if needed.
		if(isEmptyServerInInteraction(slashCommandCreateEvent) || slashCommandCreateEvent.getInteraction().getServer().isEmpty()) {
			throw new EmptyServerException();
		}

		parameters = new ConcurrentHashMap<>();
		String emptyParam;
		if((emptyParam = checkForEmptyParameter(slashCommandCreateEvent, requiredParameters)) != null) {
			throw new EmptyParameterException(emptyParam);
		}

		this.audioSession = generateOrGetAudioSession(commands);

		// get the server and user as they will be used throughout
		this.server = slashCommandCreateEvent.getInteraction().getServer().get();
		this.user = slashCommandCreateEvent.getInteraction().getUser();
	}

	public boolean isEmptyServerInInteraction(SlashCommandCreateEvent slashCommandEvent) {
		if(slashCommandEvent.getInteraction().getServer().isEmpty()) {
			Logger.warn("Server was empty in slash command interaction (this shouldn't happen!), id: " + slashCommandEvent.getInteraction().getIdAsString());
			return true;
		}
		return false;
	}
	
	/*
	 * Checks if the requesting user has a valid connection to a voice channel.
	 * The user and bot must be in the same voice, and the user must be in a voice channel.
	 */
	public boolean isBadAudioConnection(AudioSession audioSession, Server server, User user) {
		if(audioSession.hasAudioConnection() && !audioSession.isUserInSameServerVoiceChannel(user)) {
			return true;
		}
		else if(!audioSession.hasAudioConnection() && user.getConnectedVoiceChannel(server).isPresent()) {
			// the presence of ServerVoiceChannel was checked in the previous if statement
			ServerVoiceChannel serverVoiceChannel = user.getConnectedVoiceChannel(server).get();
			audioSession.setupAudioConnection(serverVoiceChannel);
			return false;
		}
		else return user.getConnectedVoiceChannel(server).isEmpty();
	}

	public String checkForEmptyParameter(SlashCommandCreateEvent slashCommandEvent, ArrayList<String> requiredParameters) {
		// return null if we dont have an arraylist (no params to check)
		if(requiredParameters == null) return null;

		for(String paramName : requiredParameters) {
			Optional<String> param = slashCommandEvent.getSlashCommandInteraction().getArgumentStringValueByName(paramName);
			if(param.isEmpty()){
				// Can "early" return with the missing parameter name
				return paramName;
			}
			else {
				parameters.put(paramName, param.get());
			}
		}

		// the early return didn't catch anything, so nothing to return as missing.
		return null;
	}

	public String getParameterValue(String key) {
		return parameters.get(key);
	}

	public User getUser() {
		return this.user;
	}

	public Server getServer() {
		return this.server;
	}

	public AudioSession getAudioSession() {
		return this.audioSession;
	}

	private AudioSession generateOrGetAudioSession(Commands commands) throws BadAudioConnectionException {
		AudioSession relevantAudioSession = commands.audioSessions
				.computeIfAbsent(server.getId(), commands::createAudioSession);
		if(isBadAudioConnection(relevantAudioSession, this.server, this.user)) {
			throw new BadAudioConnectionException();
		}
		return relevantAudioSession;
	}
}
