package SpotifyApi;

import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.io.IOException;
import java.util.ArrayList;

public class HandleSpotifyLink {
    public static ArrayList<String> getCollectionFromSpotifyLink(SpotifyApi spotifyApi, String link) throws IOException, ParseException, SpotifyWebApiException {
        String playlistIndication = "playlist/";
//        if(link.indexOf(playlistIndication) == -1) return "Not a playlist link\nIf you think this is an error, please contact <@806350925723205642>";
        if(!link.contains(playlistIndication)) return null;
        // get playlist id
        String playlistId = link.substring(link.indexOf(playlistIndication) + 9);
        // if contains ? then remove it and everything after
        if(playlistId.contains("?")) playlistId = playlistId.substring(0, playlistId.indexOf("?"));
        // if contains / then remove it and everything after
        if(playlistId.contains("/")) playlistId = playlistId.substring(0, playlistId.indexOf("/"));
        // if it contains anything that is not a letter or number then return null
//        if(!playlistId.matches("[a-zA-Z0-9]+")) return "Not a playlist link\nIf you think this is a mistake, please contact <@806350925723205642>";
        if(!playlistId.matches("[a-zA-Z0-9]+")) return null;
        ArrayList<String> tracks = new ArrayList<>();
        try {
            Playlist playlist = spotifyApi.getPlaylist(playlistId).build().execute();
            for(PlaylistTrack track : playlist.getTracks().getItems()) {
                tracks.add(((Track) track.getTrack()).getArtists()[0].getName() + " " + track.getTrack().getName());
            }
        }
        catch (Exception e) {
            try {
                // just incase some tracks were added and an error was thrown we dont want duplicates
                tracks = new ArrayList<>();
                // try refreshing the token
                spotifyApi = ClientCreate.clientCredentials_Sync();
                assert spotifyApi != null;
                Playlist playlist = spotifyApi.getPlaylist(playlistId).build().execute();
                for(PlaylistTrack track : playlist.getTracks().getItems()) {
                    tracks.add(((Track) track.getTrack()).getArtists()[0].getName() + " " + track.getTrack().getName());
                }
            }
            catch (Exception e2) {
               return tracks;
            }
        }
        return tracks;
    }
}
