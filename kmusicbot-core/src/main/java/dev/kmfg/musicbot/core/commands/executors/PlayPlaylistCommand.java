package dev.kmfg.musicbot.core.commands.executors;

import dev.kmfg.musicbot.core.sessions.SessionManager;
import dev.kmfg.musicbot.core.util.slashcommands.EnsuredSlashCommandInteraction;
import dev.kmfg.musicbot.database.models.KMusicSong;
import dev.kmfg.musicbot.database.models.Playlist;
import dev.kmfg.musicbot.database.repositories.PlaylistRepo;
import dev.kmfg.musicbot.database.util.HibernateUtil;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

public class PlayPlaylistCommand extends Command {
    public static final String COMMAND_NAME = "playlist";
    public static final String DESCRIPTION = "Play a playlist by it's name.";

    public PlayPlaylistCommand(SessionManager sessionManager, SlashCommandCreateEvent slashCommandEvent,
                                ExecutorService executorService) {
        super(sessionManager, slashCommandEvent, executorService);
    }

    public PlayPlaylistCommand() {
        super();
    }

    @Override
    public void register(DiscordApi discordApi) {
        SlashCommand.with(COMMAND_NAME, DESCRIPTION,
                        // create option(s)
                        List.of(
                                // create option <url>
                                SlashCommandOption.create(
                                        SlashCommandOptionType.STRING,
                                        "name",
                                        "Name of the playlist.",
                                        true)))
                .createGlobal(discordApi).join();
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public String getCommandDescription() {
        return DESCRIPTION;
    }

    @Override
    public void execute() {
        super.execute();
        // begin ensured interaction setup
        String playlistNameParameter = "name";
        ArrayList<String> requiredParameters = new ArrayList<>();
        requiredParameters.add(playlistNameParameter);

        EnsuredSlashCommandInteraction ensuredInteraction = getEnsuredInteraction(requiredParameters);
        // Above method will handle sending messages, stop execution here if we don't
        // get an EnsuredInteraction.
        if (ensuredInteraction == null)
            return;

        final String playlistName = ensuredInteraction.getParameterValue(playlistNameParameter);

        final PlaylistRepo playlistRepo = new PlaylistRepo(HibernateUtil.getSessionFactory());
        final Optional<Playlist> playlistOpt = playlistRepo.findByGuildAndName(
                ensuredInteraction.getServer().getId(),
                playlistName
        );

        if(playlistOpt.isEmpty()) {
            messageSender.sendNothingFoundEmbed(playlistName);
            return;
        }

        // I don't like this, need to add a deeper method to add
        //  multiple direct links via the YouTube source
        int successes = 0;
        for(KMusicSong song : playlistOpt.get().getSongs()) {
            successes += ensuredInteraction
                    .getAudioSession()
                    .queueSearchQuery(
                            this.discordUser,
                            song.getYoutubeUrl(),
                            false
                    ).isSuccess() ? 1 : 0;
        }

        final int totalTracks = playlistOpt.get().getSongs().size();
        messageSender.sendPlaylistQueueResult(playlistName, successes, totalTracks);
    }
}
