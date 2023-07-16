package DiscordApi;

import Lavaplayer.LavaplayerAudioSource;
import MySQL.SetupDatabase;
import SongRecommender.RecommenderProcessor;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class KCommands {
    private static final HashMap<Long, AudioConnection> audioConnections = new HashMap<>();
    public static HashMap<Long, Boolean> isEphemeral = new HashMap<>();

    private static long playNowCommandId = -1;

    private static RecommenderProcessor recommenderProcessor;

    public static void listenForAllCommands(DiscordApi api) {
        for(Server server : api.getServers()) {
            System.out.println("Server: " + server.getName());
            SetupDatabase.setup(server.getIdAsString());
            // messages are public by default
            isEphemeral.put(server.getId(), false);
        }
        // put processor in lavaplayer
        LavaplayerAudioSource.recommenderProcessor = recommenderProcessor;
        // handle playnow command stuff
        playNowCommandId = createGlobalPlayNowCommand(api);
        listenForPlayNowCommand(api, playNowCommandId);

        listenForPlayCommand(api);
        listenForPauseCommand(api);
        listenForStopCommand(api);
        listenForNowPlayingCommand(api);
        listenForVolumeCommand(api);
        listenForSkipCommand(api);
        listenForToggleEphemeralCommand(api);
        listenForShuffleCommand(api);
        listenForQueueCommand(api);
        listenForSeekCommand(api);
        listenForClearCommand(api);
        listenForReplayCommand(api);
        listenForPlayNextCommand(api);
    }

    private static boolean serverIsntPresentSendMsg(SlashCommandCreateEvent slashCommandCreateEvent) {
        if(slashCommandCreateEvent.getSlashCommandInteraction().getServer().isEmpty()) {
            slashCommandCreateEvent.getSlashCommandInteraction().createFollowupMessageBuilder()
                .setContent("Server not present in interaction, this shouldn't happen, but if it keeps doing it " +
                        "open an issue at https://github.com/kyleyannelli/KMusicBot/")
                .send();
            return true;
        }
        return false;
    }

    public static void addRecommenderProcessor(RecommenderProcessor rP) {
        recommenderProcessor = rP;
    }

    public static long createGlobalPlayNowCommand(DiscordApi api) {
        // create SlashCommand /play, = so the command information can be accessed prior
        SlashCommand command = SlashCommand.with("playnow", "Plays a song specified by the queue number. It will be removed from the queue upon playing.",
                // create option(s)
                Collections.singletonList(
                    // create option /play <song>
                    SlashCommandOption.create(SlashCommandOptionType.LONG, "number", "The song to play based on its number in the queue", true)
                    ))
            .createGlobal(api).join();
        return command.getId();
    }

    public static void listenForPlayNowCommand(DiscordApi api, long commandId) {
        api.addSlashCommandCreateListener(slashCommandCreateEvent -> {
            if(commandId == slashCommandCreateEvent.getSlashCommandInteraction().getCommandId()) {
                if(serverIsntPresentSendMsg(slashCommandCreateEvent)) return; 

                long serverId = slashCommandCreateEvent.getSlashCommandInteraction().getServer().get().getId();
                slashCommandCreateEvent.getInteraction()
                    .respondLater(isEphemeral.get(serverId))
                    .thenAccept(interactionAcceptance -> {
                        if(userConnectedToVc(slashCommandCreateEvent) &&
                                slashCommandCreateEvent.getSlashCommandInteraction().getArguments().get(0).getLongValue().isPresent()) {
                            int position = Math.toIntExact(slashCommandCreateEvent.getSlashCommandInteraction().getArguments().get(0).getLongValue().get());
                            switch (LavaplayerAudioSource.playNow(serverId, --position)) {
                                case 0:
                                    slashCommandCreateEvent.getSlashCommandInteraction().createFollowupMessageBuilder()
                                        .setContent("Playing track in queue position " + ++position).send();
                                    break;
                                case 1:
                                    slashCommandCreateEvent.getSlashCommandInteraction().createFollowupMessageBuilder()
                                        .setContent("The selected position, " + ++position + ", is out of bounds").send();
                                    break;
                                default:
                                    slashCommandCreateEvent.getSlashCommandInteraction().createFollowupMessageBuilder()
                                        .setContent("An unknown error occurred.").send();
                                    break;
                            }
                        } else {
                            slashCommandCreateEvent.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent("Error playing, no \"position\" argument present.");
                        }
                    });
            }
        });
    }

    public static void listenForPlayNextCommand(DiscordApi api) {
        // create SlashCommand /play, = so the command information can be accessed prior
        SlashCommand command = SlashCommand.with("playnext", "Plays a song, but puts it at the front of the queue",
                // create option(s)
                Collections.singletonList(
                    // create option /play <song>
                    SlashCommandOption.create(SlashCommandOptionType.STRING, "song", "The song to play", true)
                    ))
            .createGlobal(api).join();

        api.addSlashCommandCreateListener(slashCommandCreateEvent -> {
            if(command.getId() == slashCommandCreateEvent.getSlashCommandInteraction().getCommandId()) {
                if(serverIsntPresentSendMsg(slashCommandCreateEvent)) return; 

                long serverId = slashCommandCreateEvent.getSlashCommandInteraction().getServer().get().getId();
                slashCommandCreateEvent.getInteraction()
                    .respondLater(isEphemeral.get(serverId))
                    .thenAccept(interactionAcceptance -> {
                        if(userConnectedToVc(slashCommandCreateEvent) && slashCommandCreateEvent.getSlashCommandInteraction().getArguments().get(0).getStringValue().isPresent()) {
                            String inputSong = slashCommandCreateEvent.getSlashCommandInteraction().getArguments().get(0).getStringValue().get();
                            // set up the audio player (may already be setup this name is slightly misleading)
                            LavaplayerAudioSource.playNext(api, RunBot.spotifyApi, audioConnections.get(serverId), inputSong, slashCommandCreateEvent);
                        } else if(userConnectedToVc(slashCommandCreateEvent)) {
                            slashCommandCreateEvent.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent("Error playing, no \"song\" argument present.");
                        } else {
                            slashCommandCreateEvent.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent("You are not connected to a voice channel!");
                        }
                    });
            }
        });
    }

    public static void listenForReplayCommand(DiscordApi api) {
        SlashCommand command = SlashCommand.with("previous", "Replays the current song, or the previous song").createGlobal(api).join();

        api.addSlashCommandCreateListener(slashCommandCreateEvent -> {
            if(command.getId() == slashCommandCreateEvent.getSlashCommandInteraction().getCommandId()) {
                if(serverIsntPresentSendMsg(slashCommandCreateEvent)) return;

                long serverId = slashCommandCreateEvent.getSlashCommandInteraction().getServer().get().getId();
                slashCommandCreateEvent.getInteraction()
                    .respondLater(isEphemeral.get(serverId))
                    .thenAccept(interactionAcceptance -> {
                        handleReplayCommand(slashCommandCreateEvent, serverId);
                    });
            }
        });
    }

    private static void handleReplayCommand(SlashCommandCreateEvent slashCommandCreateEvent, long serverId) {
                        if(userConnectedToVc(slashCommandCreateEvent)) {
                            int result = LavaplayerAudioSource.replay(serverId);
                            switch(result) {
                                case -1:
                                    slashCommandCreateEvent.getSlashCommandInteraction().createFollowupMessageBuilder()
                                        .setContent("Failed to replay!").send();
                                    break;
                                case 0:
                                    slashCommandCreateEvent.getSlashCommandInteraction().createFollowupMessageBuilder()
                                        .setContent("Replaying the current track...").send();
                                    break;
                                case 1:
                                    slashCommandCreateEvent.getSlashCommandInteraction().createFollowupMessageBuilder()
                                        .setContent("Playing the previous track...").send();
                                    break;
                                default:
                                    slashCommandCreateEvent.getSlashCommandInteraction().createFollowupMessageBuilder()
                                        .setContent("An unknown error occurred...").send();
                                    break;
                            }
                        }
    }

    public static void listenForClearCommand(DiscordApi api) {
        SlashCommand command = SlashCommand.with("clear", "Clears the entire queue").createGlobal(api).join();

        api.addSlashCommandCreateListener(event -> {
            if(command.getId() == event.getSlashCommandInteraction().getCommandId()) {
                if(serverIsntPresentSendMsg(event)) return;

                long serverId = event.getSlashCommandInteraction().getServer().get().getId();
                event.getInteraction()
                    .respondLater(isEphemeral.get(serverId))
                    .thenAccept(interactionAcceptance -> {
                        if(userConnectedToVc(event)) {
                            if(LavaplayerAudioSource.clearQueue(serverId)) {
                                event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                    .setContent("Queue cleared!").send();
                            }
                            else {
                                event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                    .setContent("Queue couldn't clear :(").send();
                            }
                        }
                    });
            }
        });
    }

    public static void listenForSeekCommand(DiscordApi api) {
        ArrayList<SlashCommandOption> options = new ArrayList<>();
        options.add(SlashCommandOption.create(SlashCommandOptionType.LONG, "seconds", "The amount of seconds"));
        options.add(SlashCommandOption.create(SlashCommandOptionType.LONG, "minutes", "The amount of minutes"));
        options.add(SlashCommandOption.create(SlashCommandOptionType.LONG, "hours", "The amount of seconds"));

        SlashCommand command = SlashCommand.with("seek", "Seek to a specific position in the currently playing song",
                // create options
                options).createGlobal(api).join();

        api.addSlashCommandCreateListener(event -> {
            if(command.getId() == event.getSlashCommandInteraction().getCommandId()) {
                if(serverIsntPresentSendMsg(event)) return; 

                long serverId = event.getInteraction().getServer().get().getId();
                event.getInteraction().respondLater(isEphemeral.get(serverId)).thenAccept(slashEvent -> {
                    if(userConnectedToVc(event)) {
                        handleSeekCommand(event, serverId);
                    }
                });
            }
        });
    }

    private static void handleSeekCommand(SlashCommandCreateEvent event, long serverId) {
                        // total time in milliseconds
                        long totalRequestedTime = 0;
                        boolean minutes = false;
                        boolean seconds = false;
                        boolean hours = false;
                        for(SlashCommandInteractionOption s : event.getSlashCommandInteraction().getArguments()) {
                            if(minutes && seconds && hours) break;
                            Optional<Long> value = s.getLongValue();
                            if(value.isPresent()) {
                                switch(s.getName()) {
                                    case "seconds":
                                        if(!seconds) totalRequestedTime += value.get() * 1000;
                                        seconds = true;
                                        break;
                                    case "minutes":
                                        if(!minutes) totalRequestedTime += value.get() * 60000;
                                        minutes = true;
                                        break;
                                    case "hours":
                                        if(!hours) totalRequestedTime += value.get() * (60 * 60000);
                                        hours = true;
                                        break;
                                    default:
                                        event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                            .setContent("Please double check the parameters input as " + s.getName() + " is not supported for this command")
                                            .send();
                                        return;
                                }
                            }
                        }
                        AudioPlayer player = LavaplayerAudioSource.getPlayerByServerId(serverId);
                        long duration = player.getPlayingTrack().getDuration();
                        if(player.getPlayingTrack() == null) {
                            event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent("Nothing is playing!")
                                .send();
                            return;
                        }
                        if(duration < totalRequestedTime) {
                            System.out.println("Requested " + totalRequestedTime + " of " + duration);
                            event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent("Request seek position is beyond the current songs duration!")
                                .send();
                            return;
                        }
                        if(totalRequestedTime < 0) {
                            event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent("Requested seek position is below 0!")
                                .send();
                            return;
                        }
                        player.getPlayingTrack().setPosition(totalRequestedTime);
                        String durationString = createTimestampString(duration);
                        String requestedPosition = createTimestampString(totalRequestedTime);
                        event.getSlashCommandInteraction().createFollowupMessageBuilder()
                            .setContent("Seeking to " + requestedPosition + " **|** " + durationString)
                            .send();
    }

    private static String createTimestampString(long duration) {
        return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(duration),
                TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration)),
                TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
    }

    public static void listenForQueueCommand(DiscordApi api) {
        SlashCommand command = SlashCommand.with("queue", "Shows the current queue",
                // create option(s)
                Collections.singletonList(
                    // create option /play <song>
                    SlashCommandOption.create(SlashCommandOptionType.LONG, "page", "The number page to view, starting at 1", true)
                    ))
            // create option(s)
            .createGlobal(api)
            .join();

        api.addSlashCommandCreateListener(event -> {
            if(command.getId() == event.getSlashCommandInteraction().getCommandId()) {
                if(event.getSlashCommandInteraction().getServer().isPresent() &&
                        event.getSlashCommandInteraction().getArguments().get(0).getLongValue().isPresent()) {
                    long pageNumber = event.getSlashCommandInteraction().getArguments().get(0).getLongValue().get();
                    long serverId = event.getSlashCommandInteraction().getServer().get().getId();
                    event.getInteraction()
                        .respondLater(isEphemeral.get(serverId))
                        .thenAccept(interaction -> event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent(LavaplayerAudioSource.getQueue(serverId, pageNumber))
                                .send());
                } else if(event.getSlashCommandInteraction().getServer().isPresent()) {
                    event.getSlashCommandInteraction().createFollowupMessageBuilder()
                        .setContent("Not all arguments are present in the command. Please double check.")
                        .send();
                } else {
                    event.getSlashCommandInteraction().createFollowupMessageBuilder()
                        .setContent("Server not present in interaction, this shouldn't happen, but if it keeps doing it " +
                                "open an issue at https://github.com/kyleyannelli/KMusicBot/")
                        .send();
                }
            }
        });
    }

    public static void listenForPlayCommand(DiscordApi api) {
        // create SlashCommand /play, = so the command information can be accessed prior
        SlashCommand command = SlashCommand.with("play", "Play a song",
                // create option(s)
                Collections.singletonList(
                    // create option /play <song>
                    SlashCommandOption.create(SlashCommandOptionType.STRING, "song", "The song to play", true)
                    ))
            .createGlobal(api).join();
        // listen for command
        api.addSlashCommandCreateListener(event -> {
            // different commands, make sure the event is the one we are looking for
            if(command.getId() == event.getSlashCommandInteraction().getCommandId()) {
                event.getInteraction()
                    // this may take a while, so we need to defer the response
                    // also get if it is ephemeral (private) or not
                    .respondLater(isEphemeral.get(event.getSlashCommandInteraction().getServer().get().getId()))
                    .thenAccept(interaction -> {
                        if(event.getSlashCommandInteraction().getArguments().get(0).getStringValue().isEmpty()) {
                            event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent("Error playing, no \"song\" argument present.")
                                .send();
                            return;
                        }
                        if(serverIsntPresentSendMsg(event)) return; 
                        handlePlayCommand(api, event);
                    });
            }
        });
    }

    private static void handlePlayCommand(DiscordApi api, SlashCommandCreateEvent event) {
        // get the song name... this looks disgusting
        String inputSong = event.getSlashCommandInteraction().getArguments().get(0).getStringValue().get();
        // see if the user is in a voice channel, needs to be Atomic because it's used in a lambda
        AtomicBoolean isConnected = new AtomicBoolean(false);
        // get the user who used the command, get their voice channel, if it exists, get the audio connection
        event.getSlashCommandInteraction().getUser().getConnectedVoiceChannel(event.getSlashCommandInteraction().getServer().get()).ifPresent(voiceChannel -> {
            isConnected.set(true);
            Long serverId = event.getSlashCommandInteraction().getServer().get().getId();
            // if bot is not already connected to the voice channel
            if(!api.getYourself().isConnected(voiceChannel)) {
                // reaches here after being kicked and re-invited
                System.out.println("Bot is not connected to voice channel, connecting...");
                voiceChannel.connect().thenAccept(audioConnection -> {
                    // does not reach after bot is kicked then re-invited
                    System.out.println("Connected to voice channel " + voiceChannel.getName());
                    // put the audio connection in the hashmap
                    audioConnections.put(serverId, audioConnection);
                    // set up the audio player (may already be setup this name is slightly misleading)
                    LavaplayerAudioSource.setupAudioPlayer(api, RunBot.spotifyApi, audioConnections.get(serverId), inputSong, event, false);
                });
            }
            else {
                System.out.println("Bot is already connected to voice channel, playing song...");
                // bot is already connected to the voice channel and the audio connection is already in the hashmap
                LavaplayerAudioSource.setupAudioPlayer(api, RunBot.spotifyApi, audioConnections.get(serverId), inputSong, event, false);
            }
        });
        if (!isConnected.get()) {
            event.getSlashCommandInteraction().createFollowupMessageBuilder()
                .setContent("You must be in a voice channel to use this command!")
                .send();
        }
    }

    public static void listenForStopCommand(DiscordApi api) {
        // create SlashCommand /stop, = so the command information can be accessed prior
        SlashCommand command = SlashCommand.with("stop", "Stop playing everything")
            .createGlobal(api).join();
        // listen for command
        api.addSlashCommandCreateListener(event -> {
            // different commands, make sure the event is the one we are looking for
            if(command.getId() == event.getSlashCommandInteraction().getCommandId()) {
                if(serverIsntPresentSendMsg(event)) return; 
                handleStopCommand(api, event);
            }
        });
    }

    private static void handleStopCommand(DiscordApi api, SlashCommandCreateEvent event) {
        event.getInteraction()
            // this may take a while, so we need to defer the response
            // also get if it is ephemeral (private) or not
            .respondLater(isEphemeral.get(event.getSlashCommandInteraction().getServer().get().getId()))
            .thenAccept(interaction -> {
                // see if the user is in a voice channel, needs to be Atomic because it's used in a lambda
                AtomicBoolean isConnected = new AtomicBoolean(false);
                // get the user who used the command, get their voice channel, if it exists
                event.getSlashCommandInteraction().getUser().getConnectedVoiceChannel(event.getSlashCommandInteraction().getServer().get()).ifPresent(voiceChannel -> {
                    isConnected.set(true);
                    // disconnect from the voice channel

                    if(voiceChannel.isConnected(api.getClientId())) {
                        voiceChannel.disconnect().thenAccept(a -> {
                            // stop the song and clear the queue
                            LavaplayerAudioSource.getPlayerByServerId(event.getSlashCommandInteraction().getServer().get().getId()).stopTrack();
                            // destroy the audio player
                            LavaplayerAudioSource.getPlayerByServerId(event.getSlashCommandInteraction().getServer().get().getId()).destroy();
                            // remove the player from the hashmap, this also removes the TrackScheduler
                            LavaplayerAudioSource.removePlayerByServerId(event.getSlashCommandInteraction().getServer().get().getId());
                            // remove the session from the HashMap
                            LavaplayerAudioSource.removeRecommenderSessionByServerId(event.getSlashCommandInteraction().getServer().get().getId());
                            // let em know
                            event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent("Disconnected from voice channel!")
                                .send();
                        });
                    }
                    else {
                        // yell at them!
                        event.getSlashCommandInteraction().createFollowupMessageBuilder()
                            .setContent("I am not connected to a voice channel!")
                            .send();
                    }
                });
                // if the user is not in a voice channel
                if (!isConnected.get()) {
                    // yell at them!
                    event.getSlashCommandInteraction().createFollowupMessageBuilder()
                        .setContent("You must be in a voice channel to use this command!")
                        .send();
                }
            });
    }

    public static void listenForNowPlayingCommand(DiscordApi api) {
        // create SlashCommand /np, = so the command information can be accessed prior
        SlashCommand command = SlashCommand.with("np", "Show what is currently playing")
            .createGlobal(api).join();
        // listen for command
        api.addSlashCommandCreateListener(event -> {
            // different commands, make sure the event is the one we are looking for
            if(command.getId() == event.getSlashCommandInteraction().getCommandId()) {
                if(serverIsntPresentSendMsg(event)) return; 
                handleNowPlayingCommand(event);
            }
        });
    }

    private static void handleNowPlayingCommand(SlashCommandCreateEvent event) {
        event.getInteraction()
            // this may take a while, so we need to defer the response
            // also get if it is ephemeral (private) or not
            .respondLater(isEphemeral.get(event.getSlashCommandInteraction().getServer().get().getId()))
            .thenAccept(interaction -> {
                try {
                    if(userConnectedToVc(event)) {
                        Long serverId = event.getSlashCommandInteraction().getServer().get().getId();
                        // if the audio player is not null
                        AudioPlayer player = LavaplayerAudioSource.getPlayerByServerId(serverId);
                        long currentPosition = player.getPlayingTrack().getPosition();
                        long totalDuration = player.getPlayingTrack().getDuration();
                        String currentPositionHHMMSS = createTimestampString(currentPosition);
                        String durationHHMMSS = createTimestampString(totalDuration);
                        // let em know
                        event.getSlashCommandInteraction().createFollowupMessageBuilder()
                            .setContent("***Currently Playing:***\n" + player.getPlayingTrack().getInfo().title + "\n" + player.getPlayingTrack().getInfo().uri + "\n" +
                                    currentPositionHHMMSS + " **|** " + durationHHMMSS)
                            .send();
                    }
                }
                catch (Exception e) {
                    // yell at them!
                    event.getSlashCommandInteraction().createFollowupMessageBuilder()
                        .setContent("Nothing is currently playing!")
                        .send();
                }
            });
    }

    public static void listenForVolumeCommand(DiscordApi api) {
        // create SlashCommand /volume, = so the command information can be accessed prior
        SlashCommand command = SlashCommand.with("volume", "Change the volume of the bot",
                Collections.singletonList(
                    SlashCommandOption.create(SlashCommandOptionType.DECIMAL, "volume", "The volume to set the bot to", true)
                    ))
            .createGlobal(api).join();
        // listen for command
        api.addSlashCommandCreateListener(event -> {
            // different commands, make sure the event is the one we are looking for
            if(command.getId() == event.getSlashCommandInteraction().getCommandId()) {
                handleVolumeCommand(event);                
            }
        });
    }

    private static void handleVolumeCommand(SlashCommandCreateEvent event) {
        if(event.getSlashCommandInteraction().getArguments().get(0).getDecimalValue().isPresent() &&
                event.getSlashCommandInteraction().getServer().isPresent()) {
            /*
               @TODO: use integer instead of decimal
               */
            // i don't like this, but it works
            String inputVolume = event.getSlashCommandInteraction().getArguments().get(0).getDecimalValue().get().toString();
            int volume = Integer.parseInt(inputVolume.substring(0, inputVolume.indexOf(".")));
            event.getInteraction()
                // this may take a while, so we need to defer the response
                // also get if it is ephemeral (private) or not
                .respondLater(isEphemeral.get(event.getSlashCommandInteraction().getServer().get().getId()))
                .thenAccept(interaction -> {
                    if(userConnectedToVc(event)) {
                        Long serverId = event.getSlashCommandInteraction().getServer().get().getId();
                        // if the audio player is not null
                        AudioPlayer player = LavaplayerAudioSource.getPlayerByServerId(serverId);
                        if(player != null) {
                            // set the volume
                            player.setVolume(volume);
                            // let em know
                            event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent("Volume set to " + volume + "!")
                                .send();
                        } else {
                            // yell at them!
                            event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                .setContent("Nothing is currently playing!")
                                .send();
                        }
                    }
                });
        } else if(event.getSlashCommandInteraction().getServer().isPresent()) {
            event.getSlashCommandInteraction().createFollowupMessageBuilder()
                .setContent("Level argument not present in command...")
                .send();
        } else {
            event.getSlashCommandInteraction().createFollowupMessageBuilder()
                .setContent("Server not present in interaction, this shouldn't happen, but if it keeps doing it " +
                        "open an issue at https://github.com/kyleyannelli/KMusicBot/")
                .send();
        }
    }

    public static void listenForSkipCommand(DiscordApi api) {
        // create SlashCommand /skip, = so the command information can be accessed prior
        SlashCommand command = SlashCommand.with("skip", "Skip the current playing song")
            .createGlobal(api).join();
        // listen for command
        api.addSlashCommandCreateListener(event -> {
            // different commands, make sure the event is the one we are looking for
            if(command.getId() == event.getSlashCommandInteraction().getCommandId()) {
                if(serverIsntPresentSendMsg(event)) return; 

                event.getInteraction()
                    // this may take a while, so we need to defer the response
                    // also get if it is ephemeral (private) or not
                    .respondLater(isEphemeral.get(event.getSlashCommandInteraction().getServer().get().getId()))
                    .thenAccept(interaction -> {
                        handleSkipCommand(event);
                    });
            }
        });
    }

    private static void handleSkipCommand(SlashCommandCreateEvent event) {
        if(userConnectedToVc(event)) {
            Long serverId = event.getSlashCommandInteraction().getServer().get().getId();
            // if the audio player is not null
            AudioPlayer player = LavaplayerAudioSource.getPlayerByServerId(serverId);
            if(player != null) {
                // stop the track (effectively skipping it if there is another track in the queue)
                player.stopTrack();
                // let em know
                event.getSlashCommandInteraction().createFollowupMessageBuilder()
                    .setContent("Skipped!")
                    .send();
            } else {
                // yell at them!
                event.getSlashCommandInteraction().createFollowupMessageBuilder()
                    .setContent("Nothing is currently playing!")
                    .send();
            }
        }
    }

    public static void listenForToggleEphemeralCommand(DiscordApi api) {
        // create SlashCommand /ephemeral, = so the command information can be accessed prior
        SlashCommand command = SlashCommand.with("ephemeral", "Toggle if the bots responses are ephemeral (private)")
            .createGlobal(api).join();
        // listen for command
        api.addSlashCommandCreateListener(event -> {
            // different commands, make sure the event is the one we are looking for
            if(command.getId() == event.getSlashCommandInteraction().getCommandId()) {
                handleToggleEphemeralCommand(event); 
            }
        });
    }

    private static void handleToggleEphemeralCommand(SlashCommandCreateEvent event) {
        if(event.getSlashCommandInteraction().getServer().isPresent()) {
            Server currentServer = event.getSlashCommandInteraction().getServer().get();
            event.getInteraction()
                // this may take a while, so we need to defer the response
                // also get if it is ephemeral (private) or not
                .respondLater(isEphemeral.get(currentServer.getId()))
                .thenAccept(interaction -> {
                    // check if the user is an admin
                    if(!UserHelper.isAdmin(currentServer, event.getSlashCommandInteraction().getUser())) {
                        // yell at them!
                        event.getSlashCommandInteraction().createFollowupMessageBuilder()
                            .setContent("You must be an admin to use this command!")
                            .send();
                        return;
                    }
                    isEphemeral.put(event.getSlashCommandInteraction().getServer().get().getId(), !isEphemeral.get(event.getSlashCommandInteraction().getServer().get().getId()));
                    event.getSlashCommandInteraction().createFollowupMessageBuilder()
                        .setContent(isEphemeral.get(event.getSlashCommandInteraction().getServer().get().getId()) ? "Ephemeral is now on!" : "Ephemeral is now off!")
                        .send();
                });
        }
        else {
            event.getSlashCommandInteraction().createFollowupMessageBuilder()
                .setContent("There was an error while getting information on the server the command was send in.")
                .send();
        }
    }

    public static void listenForShuffleCommand(DiscordApi api) {
        // create SlashCommand /shuffle, = so the command information can be accessed prior
        SlashCommand command = SlashCommand.with("shuffle", "Shuffle the queue")
            .createGlobal(api).join();
        // listen for command
        api.addSlashCommandCreateListener(event -> {
            // different commands, make sure the event is the one we are looking for
            if(command.getId() == event.getSlashCommandInteraction().getCommandId()) {
                if(serverIsntPresentSendMsg(event)) return; 

                event.getInteraction()
                    // this may take a while, so we need to defer the response
                    // also get if it is ephemeral (private) or not
                    .respondLater(isEphemeral.get(event.getSlashCommandInteraction().getServer().get().getId()))
                    .thenAccept(interaction -> {
                        handleShuffleCommand(event); 
                    });
            }
        });
    }

    private static void handleShuffleCommand(SlashCommandCreateEvent event) {
        if(userConnectedToVc(event)) {
            Long serverId = event.getSlashCommandInteraction().getServer().get().getId();
            // if the audio player is not null
            if(LavaplayerAudioSource.getPlayerByServerId(serverId) != null) {
                // shuffle the queue
                LavaplayerAudioSource.shuffleQueue(serverId);
                // let em know
                event.getSlashCommandInteraction().createFollowupMessageBuilder()
                    .setContent("Shuffled!")
                    .send();
            } else {
                // yell at them!
                event.getSlashCommandInteraction().createFollowupMessageBuilder()
                    .setContent("Nothing is currently playing!")
                    .send();
            }
        }
    }

    public static void listenForPauseCommand(DiscordApi api) {
        SlashCommand command = SlashCommand.with("pause", "Pause the current track")
            .createGlobal(api).join();

        api.addSlashCommandCreateListener(event -> {
            if(command.getId() == event.getSlashCommandInteraction().getCommandId()) {
                if(serverIsntPresentSendMsg(event)) return; 

                long serverId = event.getSlashCommandInteraction().getServer().get().getId();
                event.getInteraction()
                    .respondLater(isEphemeral.get(serverId))
                    .thenAccept(interaction -> {
                        handlePauseCommand(event, serverId); 
                    });
            }
        });
    }

    private static void handlePauseCommand(SlashCommandCreateEvent event, long serverId) {
        if(userConnectedToVc(event)) {
            // if the audio player is not null
            if(LavaplayerAudioSource.getPlayerByServerId(serverId) != null) {
                // pause or unpause (depending on current state)
                if(!LavaplayerAudioSource.pause(serverId)) {
                    // let em know
                    event.getSlashCommandInteraction().createFollowupMessageBuilder()
                        .setContent("Resumed! (music was already paused)")
                        .send();
                }
                else {
                    // let em know
                    event.getSlashCommandInteraction().createFollowupMessageBuilder()
                        .setContent("Paused!")
                        .send();
                }
            } else {
                // yell at them!
                event.getSlashCommandInteraction().createFollowupMessageBuilder()
                    .setContent("Nothing is currently playing!")
                    .send();
            }
        }
    }

    private static boolean userConnectedToVc(SlashCommandCreateEvent event) {
        AtomicBoolean isConnected = new AtomicBoolean(false);
        // get the user who used the command, get their voice channel, if it exists
        if(event.getSlashCommandInteraction().getServer().isPresent()) {
            event.getSlashCommandInteraction().getUser().getConnectedVoiceChannel(event.getSlashCommandInteraction().getServer().get()).ifPresent(voiceChannel -> isConnected.set(true));
            if (!isConnected.get()) {
                // yell at them!
                event.getSlashCommandInteraction().createFollowupMessageBuilder()
                    .setContent("You must be in a voice channel to use this command!")
                    .send();
            }
        }
        return isConnected.get();
    }
}
