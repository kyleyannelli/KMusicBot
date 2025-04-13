package dev.kmfg.musicbot.core.commands.executors;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.javacord.api.DiscordApi;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.tinylog.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.kmfg.musicbot.core.util.slashcommands.EnsuredSlashCommandInteraction;
import dev.kmfg.musicbot.database.models.DiscordGuild;
import dev.kmfg.musicbot.database.models.KMusicSong;
import dev.kmfg.musicbot.database.models.Playlist;
import dev.kmfg.musicbot.database.repositories.KMusicSongRepo;
import dev.kmfg.musicbot.database.repositories.PlaylistRepo;
import dev.kmfg.musicbot.database.util.HibernateUtil;

public class AddToPlaylist extends Command {
    public static final String COMMAND_NAME = "add";
    public static final String DESCRIPTION = "Add a YouTube link to a playlist.";

    private static final String VIDEO_DETAIL_API_FORMAT = "https://www.youtube.com/oembed?url=%s&format=json";
    private static final String YOUTUBE_VIDEO_URL = "https://www.youtube.com/v=%s";

    @Override
    public void register(DiscordApi discordApi) {
        SlashCommand.with(COMMAND_NAME, DESCRIPTION,
                // create option(s)
                List.of(
                        // create option <url>
                        SlashCommandOption.create(
                                SlashCommandOptionType.STRING,
                                "url",
                                "The YouTube url to add to the playlist",
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
        String songParameter = "url";
        ArrayList<String> requiredParameters = new ArrayList<>();
        requiredParameters.add(songParameter);

        EnsuredSlashCommandInteraction ensuredInteraction = getEnsuredInteraction(requiredParameters);
        // Above method will handle sending messages, stop execution here if we don't
        // get an EnsuredInteraction.
        if (ensuredInteraction == null)
            return;

        final String youtubeUrl = ensuredInteraction.getParameterValue(songParameter);
        final Optional<KMusicSong> songOpt = getYouTubeVideoDetails(youtubeUrl);
        if (songOpt.isEmpty()) {
            this.messageSender.sendNothingFoundEmbed(youtubeUrl);
            return;
        }

        final KMusicSong song = songOpt.get();
        final List<Playlist> playlists = getPlaylists(ensuredInteraction.getServer().getId());
        this.messageSender.sendAddToPlaylistEmbed(song, playlists);
    }

    private Optional<KMusicSong> getYouTubeVideoDetails(final String url) {
        final String apiUrl = String.format(VIDEO_DETAIL_API_FORMAT, url);
        final HttpClient httpClient = HttpClient.newHttpClient();
        final HttpRequest httpRequest = HttpRequest
                .newBuilder(URI.create(apiUrl))
                .build();

        HttpResponse<String> response = null;
        try {
            response = httpClient.send(httpRequest, BodyHandlers.ofString());
        } catch (InterruptedException ie) {
            Logger.error(ie, "Add to playlist YouTube API request interrupted!");
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (IOException ioe) {
            Logger.error(ioe, "Add to playlist YouTube API request failed due to IOE!");
        }

        if (response == null) {
            Logger.warn("Add to playlist HTTP client did not throw, but response is null!");
            return Optional.empty();
        } else if (response.statusCode() != 200) {
            Logger.warn("Add to playlist HTTP client gave %d status code!", response.statusCode());
            return Optional.empty();
        }

        final String responseBody = response.body();
        final ObjectMapper mapper = new ObjectMapper();

        JsonNode rootNode = null;
        try {
            rootNode = mapper.readTree(responseBody);
        } catch (JsonMappingException jme) {
            Logger.error(jme, "Add to playlist could not parse response body\n\t%s!", responseBody);
            return Optional.empty();
        } catch (JsonProcessingException jpe) {
            Logger.error(jpe, "Add to playlist could not parse response body\n\t%s!", responseBody);
            return Optional.empty();
        }

        if (rootNode == null) {
            Logger.warn("Add to playlist JsonMapper did not throw, but rootNode is null!");
            return Optional.empty();
        }

        final String title = rootNode.get("title").asText();
        if (title == null || title.isBlank()) {
            Logger.error("Add to playlist received valid response but title is null or blank!");
            return Optional.empty();
        }
        final String author = rootNode.get("author").asText();
        if (author == null || author.isBlank()) {
            Logger.error("Add to playlist received valid response but author is null or blank!");
            return Optional.empty();
        }

        final String[] urlSplit = url.split("v=");
        if (urlSplit.length < 2) {
            Logger.error("Add to playlist url does not contain v= but was found by youtube API. We were given %s", url);
            return Optional.empty();
        } else if (urlSplit[1].length() < 11) {
            Logger.error("Add to playlist url does contains v= but the ID is not long enough. We were given %s", url);
            return Optional.empty();
        }

        final String vID = urlSplit[1].substring(0, 11);
        final String sanitizedYouTubeUrl = String.format(YOUTUBE_VIDEO_URL, vID);
        final KMusicSong kmusicSong = new KMusicSong(sanitizedYouTubeUrl, author, title);
        return saveSong(kmusicSong);
    }

    private Optional<KMusicSong> saveSong(KMusicSong song) {
        final KMusicSongRepo songRepo = new KMusicSongRepo(HibernateUtil.getSessionFactory());
        return Optional.of(songRepo.saveOrGet(song));
    }

    private List<Playlist> getPlaylists(long guildId) {
        final PlaylistRepo playlistRepo = new PlaylistRepo(HibernateUtil.getSessionFactory());
        return playlistRepo.findByGuild(new DiscordGuild(guildId));
    }
}
