package dev.kmfg.musicbot.core.spotifyapi;

import io.github.cdimascio.dotenv.Dotenv;
import org.tinylog.Logger;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.util.Optional;

/**
 * Given that the Spotify Web API was abruptly discontinued, these classes are
 * deprecated.
 */
@Deprecated
public class ClientCreate {
    private static final String clientId = Dotenv.load().get("SPOTIFY_CLIENT_ID");
    private static final String clientSecret = Dotenv.load().get("SPOTIFY_SECRET_ID");

    private static final SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .build();
    private static final ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials()
            .build();

    public static Optional<SpotifyApi> generateClientCredentials() {
        try {
            final ClientCredentials clientCredentials = clientCredentialsRequest.execute();
            // Set access token for further "spotifyApi" object usage
            spotifyApi.setAccessToken(clientCredentials.getAccessToken());
            Logger.info("Created spotify access token, expires in " + clientCredentials.getExpiresIn());
            return Optional.of(spotifyApi);
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            Logger.error("Error creating spotify API client credentials: " + e.getMessage());
            return Optional.empty();
        }
    }
}
