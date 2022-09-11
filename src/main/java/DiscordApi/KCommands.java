package DiscordApi;

import Lavaplayer.LavaplayerAudioSource;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class KCommands {
    private static HashMap<Long, AudioConnection> audioConnections = new HashMap<>();
    public static HashMap<Long, Boolean> isEphemeral = new HashMap<>();
    public static void listenForAllCommands(DiscordApi api) {
        for(Server server : api.getServers()) {
            // messages are public by default
            isEphemeral.put(server.getId(), false);
        }
        listenForPlayCommand(api);
        listenForStopCommand(api);
        listenForNowPlayingCommand(api);
        listenForVolumeCommand(api);
        listenForSkipCommand(api);
        listenForToggleEphemeralCommand(api);
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
                // get the song name... this looks disgusting
                String inputSong = event.getSlashCommandInteraction().getArguments().get(0).getStringValue().get();
                event.getInteraction()
                        // this may take a while, so we need to defer the response
                        // also get if it is ephemeral (private) or not
                        .respondLater(isEphemeral.get(event.getSlashCommandInteraction().getServer().get().getId()))
                        .thenAccept(interaction -> {
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
                                        LavaplayerAudioSource.setupAudioPlayer(api, audioConnections.get(serverId), inputSong, event);
                                    });
                                }
                                else {
                                    System.out.println("Bot is already connected to voice channel, playing song...");
                                    // bot is already connected to the voice channel and the audio connection is already in the hashmap
                                    LavaplayerAudioSource.setupAudioPlayer(api, audioConnections.get(serverId), inputSong, event);
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
        });
    }
    public static void listenForStopCommand(DiscordApi api) {
        // create SlashCommand /stop, = so the command information can be accessed prior
        SlashCommand command = SlashCommand.with("stop", "Stop playing everything")
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
                            // see if the user is in a voice channel, needs to be Atomic because it's used in a lambda
                            AtomicBoolean isConnected = new AtomicBoolean(false);
                            // get the user who used the command, get their voice channel, if it exists
                            event.getSlashCommandInteraction().getUser().getConnectedVoiceChannel(event.getSlashCommandInteraction().getServer().get()).ifPresent(voiceChannel -> {
                                isConnected.set(true);
                                // disconnect from the voice channel

                                if(voiceChannel.isConnected(api.getClientId())) {
                                    voiceChannel.disconnect().thenAccept(a -> {
                                        // destroy the audio player
                                        LavaplayerAudioSource.getPlayerByServerId(event.getSlashCommandInteraction().getServer().get().getId()).destroy();
                                        // remove the player from the hashmap, this also removes the TrackScheduler
                                        LavaplayerAudioSource.removePlayerByServerId(event.getSlashCommandInteraction().getServer().get().getId());
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
                                Long serverId = event.getSlashCommandInteraction().getServer().get().getId();
                                // if the audio player is not null
                                AudioPlayer player = LavaplayerAudioSource.getPlayerByServerId(serverId);
                                if(player != null) {
                                    // let em know
                                    event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                            .setContent("***Currently Playing:***\n" + player.getPlayingTrack().getInfo().title + "\n" + player.getPlayingTrack().getInfo().uri)
                                            .send();
                                } else {
                                    // yell at them!
                                    event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                            .setContent("Nothing is currently playing!")
                                            .send();
                                }
                            });
                            if (!isConnected.get()) {
                                // yell at them!
                                event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                        .setContent("You must be in a voice channel to use this command!")
                                        .send();
                            }
                        });
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
                            // see if the user is in a voice channel, needs to be Atomic because it's used in a lambda
                            AtomicBoolean isConnected = new AtomicBoolean(false);
                            // get the user who used the command, get their voice channel, if it exists
                            event.getSlashCommandInteraction().getUser().getConnectedVoiceChannel(event.getSlashCommandInteraction().getServer().get()).ifPresent(voiceChannel -> {
                                isConnected.set(true);
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
        });
    }

    public static void listenForSkipCommand(DiscordApi api) {
        // create SlashCommand /skip, = so the command information can be accessed prior
        SlashCommand command = SlashCommand.with("skip", "Skip the current playing song")
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
                            // see if the user is in a voice channel, needs to be Atomic because it's used in a lambda
                            AtomicBoolean isConnected = new AtomicBoolean(false);
                            // get the user who used the command, get their voice channel, if it exists
                            event.getSlashCommandInteraction().getUser().getConnectedVoiceChannel(event.getSlashCommandInteraction().getServer().get()).ifPresent(voiceChannel -> {
                                isConnected.set(true);
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
        });
    }

    public static void listenForToggleEphemeralCommand(DiscordApi api) {
        // create SlashCommand /ephemeral, = so the command information can be accessed prior
        SlashCommand command = SlashCommand.with("ephemeral", "Toggle if the bots responses are ephemeral (private)")
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
                            isEphemeral.put(event.getSlashCommandInteraction().getServer().get().getId(), !isEphemeral.get(event.getSlashCommandInteraction().getServer().get().getId()));
                            event.getSlashCommandInteraction().createFollowupMessageBuilder()
                                    .setContent(isEphemeral.get(event.getSlashCommandInteraction().getServer().get().getId()) ? "Ephemeral is now on!" : "Ephemeral is now off!")
                                    .send();
                        });
            }
        });
    }

    public static void listenForShuffleCommand(DiscordApi api) {
        // create SlashCommand /shuffle, = so the command information can be accessed prior
        SlashCommand command = SlashCommand.with("shuffle", "Shuffle the queue")
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
                            // see if the user is in a voice channel, needs to be Atomic because it's used in a lambda
                            AtomicBoolean isConnected = new AtomicBoolean(false);
                            // get the user who used the command, get their voice channel, if it exists
                            event.getSlashCommandInteraction().getUser().getConnectedVoiceChannel(event.getSlashCommandInteraction().getServer().get()).ifPresent(voiceChannel -> {
                                isConnected.set(true);
                                Long serverId = event.getSlashCommandInteraction().getServer().get().getId();
                                // if the audio player is not null
                                AudioPlayer player = LavaplayerAudioSource.getPlayerByServerId(serverId);
                                if(player != null) {
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
        });
    }
}
