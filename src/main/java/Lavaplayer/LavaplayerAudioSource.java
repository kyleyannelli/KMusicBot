package Lavaplayer;

import MySQL.Songs;
import MySQL.Users;
import SpotifyApi.HandleSpotifyLink;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.audio.AudioSourceBase;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import se.michaelthelin.spotify.SpotifyApi;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Executors;

import SpotifyApi.RadioSongsHandler;

public class LavaplayerAudioSource extends AudioSourceBase {

    private final AudioPlayer audioPlayer;
    private AudioFrame lastFrame;
    private static final ConcurrentHashMap<Long, AudioPlayer> players = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, TrackScheduler> schedulers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, ArrayList<String>> searchedSongs = new ConcurrentHashMap<>();
    private static final ExecutorService execServ = Executors.newFixedThreadPool(1); 

    public static ConcurrentHashMap<Long, Timer> timers = new ConcurrentHashMap<>();

    /**
     * Creates a new lavaplayer audio source.
     *
     * @param api A discord api instance.
     * @param audioPlayer An audio player from Lavaplayer.
     */
    public LavaplayerAudioSource(DiscordApi api, AudioPlayer audioPlayer) {
        super(api);
        this.audioPlayer = audioPlayer;
    }

    public static void removePlayerByServerId(long id) {
        players.remove(id);
    }

    public static void shuffleQueue(Long serverId) {
        Collections.shuffle(schedulers.get(serverId).audioQueue);
    }

    @Override
    public byte[] getNextFrame() {
        if (lastFrame == null) {
            return null;
        }
        return applyTransformers(lastFrame.getData());
    }

    @Override
    public boolean hasFinished() {
        return false;
    }

    @Override
    public boolean hasNextFrame() {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Override
    public AudioSource copy() {
        return new LavaplayerAudioSource(getApi(), audioPlayer);
    }

    /**
     * returns 0 on success, 1 on queue position too large, -1 on unknown error
     * @param serverId, id of the discord server
     * @param position, position in the current queue for said discord server
     * @return gives result of playnow as it may not always be applicable. This could turn into a boolean...
     */
    public static int playNow(long serverId, int position) {
        long positionL = Long.parseLong(Integer.toString(position));
        try {
            AudioTrack track = schedulers.get(serverId).audioQueue.get(position).makeClone();
            players.get(serverId).playTrack(track);
            players.remove(positionL);
            return 0;
        }
        catch (Exception e) {
            System.out.println(e.getMessage() + "POSITION AS INT " + position + " POSITION AS LONG " + positionL);
            return -1;
        }
    }

    public static void playNext(DiscordApi api, SpotifyApi spotifyApi, AudioConnection audioConnection, String url, SlashCommandCreateEvent event) {
        setupAudioPlayer(api, spotifyApi, audioConnection, url, event, true);
    }

    private static void loadRecommendedTracks(DiscordApi api, SpotifyApi spotifyApi, AudioPlayerManager playerManager, AudioConnection audioConnection, SlashCommandCreateEvent event) {
        // Retrieve audio queue from the server
        ArrayList<AudioTrack> audioQueue = schedulers.get(event.getSlashCommandInteraction().getServer().get().getId()).audioQueue;

        // Calculate the total duration of all songs in the queue
        AtomicLong totalDuration = new AtomicLong();
        audioQueue.forEach(song -> totalDuration.addAndGet(song.getDuration()));
        System.out.println("Total duration: " + totalDuration.get());
        // If the total duration of the audio queue is more than 900 OR queue size is between 4 and 15 (both inclusive)
        if(totalDuration.get() > 900 || (audioQueue.size() >= 4 && audioQueue.size() <= 15)) {
            // Process the songs from searched songs list
            ArrayList<String> trackNames = processSearchedSongs(event, audioQueue);

            // Try to generate recommendations from Spotify
            try {
                String[] recommendedTracks = RadioSongsHandler.generateRecommendationsFromList(spotifyApi, trackNames);

                // If there are no recommended tracks, return
                if(recommendedTracks == null) return;

                // Add recommended tracks to queue via YouTube
                for(String track : recommendedTracks) {
                    Thread.sleep(1000);
                    entirelyLoadTrack(api, playerManager, audioConnection, event, "ytsearch:" + track, true, false);
                }
            } catch (Exception e) {
                return;
            }
        }
    }

    private static ArrayList<String> processSearchedSongs(SlashCommandCreateEvent event, ArrayList<AudioTrack> audioQueue) {
        ArrayList<String> trackNames = new ArrayList<>();
        ArrayList<String> searchedSongsList = searchedSongs.get(event.getSlashCommandInteraction().getServer().get().getId());

        for(Object song : searchedSongsList) {
            String songStr = song.toString();
            if(songStr.startsWith("https://www.youtube") || songStr.startsWith("https://youtu.be") || songStr.startsWith("https://youtube")) {
                // If it's a YouTube URL, check if it's already in the queue
                for(AudioTrack track : audioQueue) {
                    if(track.getInfo().uri.equals(songStr)) {
                        trackNames.add(track.getInfo().title);
                    }
                }
            } else {
                // If it's not a YouTube URL, simply add it to trackNames
                trackNames.add(songStr);
            }
        }
        return trackNames;
    }

    private static void handleSongSearchTracking(long serverId, String newSearchQuery) {
        ArrayList<String> searchedSongsList;
        if(searchedSongs.containsKey(serverId)) {
            searchedSongsList = searchedSongs.get(serverId);
        }
        else {
            searchedSongsList = new ArrayList<>();
        }
        // if list is 10 or more remove the first element
        if(searchedSongsList.size() >= 10) {
            searchedSongsList.remove(0);
        }
        // add the new search to the list
        searchedSongsList.add(newSearchQuery);
        // put the list back into the map
        searchedSongs.put(serverId, searchedSongsList);
    }

    public static void setupAudioPlayer(DiscordApi api, SpotifyApi spotifyApi, AudioConnection audioConnection, String url, SlashCommandCreateEvent event, boolean next) {
        if(event.getSlashCommandInteraction().getServer().isEmpty()) {
            sendMessageStc("Server not present in interaction, this shouldn't happen, but if it keeps doing it " +
                    "open an issue at https://github.com/kyleyannelli/KMusicBot/", event);
            return;
        }
        if(timers.get(event.getSlashCommandInteraction().getServer().get().getId()) == null) {
            // get server id
            long serverId = event.getSlashCommandInteraction().getServer().get().getId();
            // start timer for 5 minutes
            createDisconnectTimer(api, serverId, new Timer());
        }
        else {
            // get the timer and reset it
            Timer timer = timers.get(event.getSlashCommandInteraction().getServer().get().getId());
            timer.cancel();
            timer.purge();
            // get server id
            long serverId = event.getSlashCommandInteraction().getServer().get().getId();
            createDisconnectTimer(api, serverId, new Timer());
        }
        // check if it's a YouTube link or search query
        AudioPlayerManager playerManager = createYouTubePlayerManager();
        handleSongSearchTracking(event.getSlashCommandInteraction().getServer().get().getId(), url);
        if(schedulers.containsKey(event.getSlashCommandInteraction().getServer().get().getId())) {
            // submit this to the thread service, this should be limited due to rate limiting and posibility of too many overlaps
            execServ.submit(() -> loadRecommendedTracks(api, spotifyApi, playerManager, audioConnection, event));
        }
        if(url.startsWith("https://www.youtube.com/")) {
            // Create a player manager
            entirelyLoadTrack(api, playerManager, audioConnection, event, url, false, next);
        }
        else if(url.startsWith("https://open.spotify.com/")) {
            Collection<String> trackNames;
            try {
                trackNames = HandleSpotifyLink.getCollectionFromSpotifyLink(spotifyApi, url);
                if(trackNames == null) {
                    sendMessageStc("Not a playlist link\nIf you think this is an error, please contact <@806350925723205642>",
                            event.getSlashCommandInteraction().getServer().get().getId(),
                            event);
                }
                else if(trackNames.size() == 0) {
                    sendMessageStc("Not a playlist link\nIf you think this is an error, please contact <@806350925723205642>",
                            event.getSlashCommandInteraction().getServer().get().getId(),
                            event);
                }
            }
            catch (Exception e) {
                sendMessageStc("Not a playlist link\nIf you think this is an error, please contact <@806350925723205642>",
                        event.getSlashCommandInteraction().getServer().get().getId(),
                        event);
                return;
            }
            try {
                setAudioPlayer(api, audioConnection, playerManager, event);
                long serverId = event.getSlashCommandInteraction().getServer().get().getId();
                assert trackNames != null;
                for(String trackName : trackNames) {
                    playerManagerLoadTrack(playerManager, "ytsearch:" + trackName, event, serverId, false, true, next);
                }
                LavaplayerAudioSource.sendMessageStc( "Added " + trackNames.size() + " tracks to the queue",
                        serverId,
                        event);
            }
            catch (Exception e) {
                sendMessageStc("An error occurred while adding the spotify tracks to the queue\nPlease try again.",
                        event.getSlashCommandInteraction().getServer().get().getId(),
                        event);
            }
        }
        else {
            entirelyLoadTrack(api, playerManager, audioConnection, event, "ytsearch:" + url, true, next);
        }
    }

    public static void createDisconnectTimer(DiscordApi api, long serverId, Timer timer) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(players.get(serverId).getPlayingTrack() == null &&
                        api.getServerById(serverId).isPresent()) {
                    Server server = api.getServerById(serverId).get();
                    if(api.getYourself().getConnectedVoiceChannel(server).isPresent()) {
                        api.getYourself().getConnectedVoiceChannel(server).get().disconnect();
                        removePlayerByServerId(serverId);
                        timers.remove(serverId);
                    }
                        }
                else if(api.getServerById(serverId).isPresent()){
                    createDisconnectTimer(api, serverId, new Timer());
                }
            }
        }, 300000);
        timers.put(serverId, timer);
    }

    public static AudioPlayer getPlayerByServerId(Long serverId) {
        return players.get(serverId);
    }

    private static AudioSource updatePlayerAndCreateAudioSource(DiscordApi api, AudioPlayerManager playerManager, Long serverId) {
        if(!players.containsKey(serverId)) {
            players.put(serverId, playerManager.createPlayer());
            schedulers.put(serverId, new TrackScheduler(serverId, api));
            players.get(serverId).addListener(schedulers.get(serverId));
        }

        // Create an audio source and add it to the audio connection's queue
        return new LavaplayerAudioSource(api, players.get(serverId));
    }

    private static AudioPlayerManager createYouTubePlayerManager() {
        // Create a player manager
        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        return playerManager;
    }

    private static void playerManagerLoadTrack(AudioPlayerManager playerManager, String url, SlashCommandCreateEvent event, long serverId, boolean sendFollowupMessage, boolean isSearch, boolean next) {
        playerManager.loadItem(url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                saveSong(track, serverId, event);
                if(players.get(serverId).getPlayingTrack() == null) {
                    players.get(serverId).playTrack(track);
                    if(sendFollowupMessage) {
                        sendMessageStc("***Now Playing:*** \n" + track.getInfo().title + "\n<" + track.getInfo().uri + ">" , serverId, event);
                    }
                } else {
                    if(next) schedulers.get(serverId).queueNext(track);
                    else schedulers.get(serverId).queue(track);
                    if(sendFollowupMessage) {
                        sendMessageStc("***Queued:*** \n" + track.getInfo().title + "\n<" + track.getInfo().uri + ">" , serverId, event);
                    }
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if(players.get(serverId).getPlayingTrack() == null) {
                    if(isSearch) {
                        // play the first track
                        players.get(serverId).playTrack(playlist.getTracks().get(0));
                        long userId;
                        try {
                            String serverIdString = Long.toString(serverId);
                            Songs s = new Songs(playlist.getTracks().get(0).getInfo().title, playlist.getTracks().get(0).getInfo().author, playlist.getTracks().get(0).getInfo().uri);
                            long songId = s.save(serverIdString);
                            System.out.println("songId: " + songId);
                            Users user = new Users(event.getSlashCommandInteraction().getUser().getId(), serverId, songId, 0);
                            userId = user.save(serverIdString);
                            schedulers.get(serverId).userDiscordIdRequestedSongId.put(playlist.getTracks().get(0).getInfo().identifier, userId);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Error while saving song to database, continuing...");
                        }
                        if(sendFollowupMessage) {
                            sendMessageStc("***Now Playing:*** \n" + playlist.getTracks().get(0).getInfo().title + "\n<" + playlist.getTracks().get(0).getInfo().uri + ">", serverId, event);
                        }
                    }
                    else {
                        for(int i = 1; i < playlist.getTracks().size(); i++) {
                            if(next) schedulers.get(serverId).queueNext(playlist.getTracks().get(i));
                            else schedulers.get(serverId).queue(playlist.getTracks().get(i));
                        }
                        players.get(serverId).playTrack(playlist.getTracks().get(0));
                        if(sendFollowupMessage) {
                            sendMessageStc("***Now Playing:*** \n" + playlist.getTracks().get(0).getInfo().title + "\n<" + playlist.getTracks().get(0).getInfo().uri + ">\n" + "***Queued " + (schedulers.get(serverId).audioQueue.size() - 1) + " Tracks...*** \n" + url, serverId, event);
                        }
                    }
                } else {
                    if(isSearch) {
                        long userId;
                        try {
                            String serverIdString = Long.toString(serverId);
                            Songs s = new Songs(playlist.getTracks().get(0).getInfo().title, playlist.getTracks().get(0).getInfo().author, playlist.getTracks().get(0).getInfo().uri);
                            long songId = s.save(serverIdString);
                            Users user = new Users(event.getSlashCommandInteraction().getUser().getId(), serverId, songId, 0);
                            userId = user.save(serverIdString);
                            schedulers.get(serverId).userDiscordIdRequestedSongId.put(playlist.getTracks().get(0).getInfo().identifier, userId);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Error while saving song to database, continuing...");
                        }
                        // play the first track
                        if(next) schedulers.get(serverId).queueNext(playlist.getTracks().get(0));
                        else schedulers.get(serverId).queue(playlist.getTracks().get(0));
                        if(sendFollowupMessage) {
                            sendMessageStc("***Queued:*** \n" + playlist.getTracks().get(0).getInfo().title + "\n<" + playlist.getTracks().get(0).getInfo().uri + ">", serverId, event);
                        }
                    }
                    else {
                        for(AudioTrack track : playlist.getTracks()) {
                            saveSong(track, serverId, event);
                            if(next) schedulers.get(serverId).queueNext(track);
                            else schedulers.get(serverId).queue(track);
                        }
                        if(sendFollowupMessage) {
                            sendMessageStc("Queued " + (schedulers.get(serverId).audioQueue.size() - 1) + " Tracks...", serverId, event);
                        }
                    }
                }
            }

            @Override
            public void noMatches() {
                if(sendFollowupMessage) {
                    // Notify the user that we've got nothing
                    sendMessageStc("Nothing found by <" + url + ">", serverId, event);
                }
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                if(sendFollowupMessage) {
                    // Notify the user that everything exploded
                    sendMessageStc("Could not play: " + exception.getMessage(), serverId, event);
                }
            }
        });
    }

    private static void saveSong(AudioTrack track, long serverId, SlashCommandCreateEvent event) {
        long userId;
        try {
            String serverIdString = Long.toString(serverId);
            Songs s = new Songs(track.getInfo().title, track.getInfo().author, track.getInfo().uri);
            long songId = s.save(serverIdString);
            Users user = new Users(event.getSlashCommandInteraction().getUser().getId(), serverId, songId, 0);
            userId = user.save(serverIdString);
            schedulers.get(serverId).userDiscordIdRequestedSongId.put(track.getInfo().identifier, userId);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error while saving song to database, continuing...");
        }
    }

    private static void entirelyLoadTrack(DiscordApi api, AudioPlayerManager playerManager, AudioConnection audioConnection, SlashCommandCreateEvent event, String url, boolean isSearch, boolean next) {
        setAudioPlayer(api, audioConnection, playerManager, event);
        if(event.getSlashCommandInteraction().getServer().isEmpty()) {
            sendMessageStc("Server not present in interaction, this shouldn't happen, but if it keeps doing it " +
                    "open an issue at https://github.com/kyleyannelli/KMusicBot/", event);
            return;
        }
        // Load the track
        playerManagerLoadTrack(playerManager, url, event, event.getSlashCommandInteraction().getServer().get().getId(), true, isSearch, next);
    }

    private static void setAudioPlayer(DiscordApi api, AudioConnection audioConnection, AudioPlayerManager playerManager, SlashCommandCreateEvent event) {
        long serverId;
        if(event.getSlashCommandInteraction().getServer().isEmpty()) {
            sendMessageStc("Server not present in interaction, this shouldn't happen, but if it keeps doing it " +
                    "open an issue at https://github.com/kyleyannelli/KMusicBot/", event);
            return;
        }
        serverId = event.getSlashCommandInteraction().getServer().get().getId();
        audioConnection.setAudioSource(updatePlayerAndCreateAudioSource(api, playerManager, serverId));
    }

    public static String getQueue(long serverId, long pageNumber) {
        if(pageNumber < 1) {
            pageNumber = 1;
        }
        pageNumber--;
        // show 5 tracks per page
        StringBuilder queue = new StringBuilder();
        try {
            int start = (int) (pageNumber * 5);
            int end = (start + 5);
            // if start is greater than queue size show last page
            if(start > schedulers.get(serverId).audioQueue.size()) {
                start = schedulers.get(serverId).audioQueue.size() - 5;
                end = schedulers.get(serverId).audioQueue.size();
            }
            if(start > schedulers.get(serverId).audioQueue.size()) {
                return "No tracks in queue.";
            }
            if(end > schedulers.get(serverId).audioQueue.size()) {
                end = schedulers.get(serverId).audioQueue.size();
            }
            for(int i = start; i < end; i++) {
                queue.append(i + 1).append(". ").append(schedulers.get(serverId).audioQueue.get(i).getInfo().title).append("\n");
            }
        }
        catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return "There was an error getting the queue.";
        }
        return queue.toString();
    }

    public static boolean pause(long serverId) {
        if(players.get(serverId).isPaused()) {
            players.get(serverId).setPaused(false);
            return false;
        }
        else {
            players.get(serverId).setPaused(true);
            return true;
        }
    }

    public static boolean clearQueue(long serverId) {
        try {
            schedulers.get(serverId).audioQueue.clear();
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
    /**
     * Returns 0 if it replays the current track, 1 if it plays the previous, and -1 if it failed...
     */
    public static int replay(long serverId) {
        try {
            AudioTrack nowPlaying = players.get(serverId).getPlayingTrack();
            if(nowPlaying.getPosition() > (1000 * 10)) {
                players.get(serverId).getPlayingTrack().setPosition(0L);
                players.get(serverId).setPaused(false);
                return 0;
            }
            else {
                players.get(serverId).playTrack(schedulers.get(serverId).lastTrack.makeClone());
                players.get(serverId).setPaused(false);
                return 1;
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            return -1;
        }
    }

    /**
     * Messages should always include the pause message if the bot is paused. To avoid nesting, func is used.
     *  Just uses the followup message builder on slash command interaction!
     *  Requires the player to be in the players hashmap.
     * @param msgContent Content of the message. Just use your regular message, the pause message will be added if needed.
     * @param serverId Server ID of the server the message is being sent to.
     * @param event SlashCommandCreateEvent so the function can actually send the message.
     */
    public static void sendMessageStc(String msgContent, long serverId, SlashCommandCreateEvent event) {
        if(players.containsKey(serverId) && players.get(serverId).isPaused()) {
            msgContent += "\n***I'm paused! To unpause type /pause***";
        }
        sendMessageStc(msgContent, event);
    }

    public static void sendMessageStc(String msgContent, SlashCommandCreateEvent event) {
        event.getSlashCommandInteraction().createFollowupMessageBuilder()
            .setContent(msgContent)
            .send();
    }
}

