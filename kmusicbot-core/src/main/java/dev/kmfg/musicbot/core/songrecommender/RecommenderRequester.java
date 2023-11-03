package dev.kmfg.musicbot.core.songrecommender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hc.core5.http.ParseException;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;

import org.tinylog.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovisionaries.i18n.CountryCode;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

public class RecommenderRequester {
	// Can make a maximum of 180 requests to Spotify
    private final int MAX_REQUESTS_PER_MIN = 100;
	// Start backing off when we are at 90% of the MAX_REQUESTS_PER_MIN
	private final int MAX_REQUEST_THRESHOLD = 90;
	// Delay to API in milliseconds
	private final int BASE_REQUEST_DELAY_MS = 100;
	private int nextRequestDelayMs = BASE_REQUEST_DELAY_MS;

    private final AtomicInteger lastMinuteRequestCount;
	private final ScheduledExecutorService scheduler;
	private final SpotifyApi spotifyApi;

	public RecommenderRequester(SpotifyApi spotifyApi) {
		this.spotifyApi = spotifyApi;

		this.lastMinuteRequestCount = new AtomicInteger(0);
		this.scheduler = Executors.newSingleThreadScheduledExecutor();

		// every minute reset lastMinuteRequestCount to 0
		this.scheduler.scheduleAtFixedRate(() -> lastMinuteRequestCount.set(0), 1, 1, TimeUnit.MINUTES);
	}



	public void possiblyBackOffApiCallAndUpdateDelay() {
		try {
			Thread.sleep(nextRequestDelayMs);
		}
		catch(InterruptedException iE) {
			// let it be interruptted
			Thread.currentThread().interrupt();
		}
		if(isNearingRateLimit()) {
			int previousRequestDelay = nextRequestDelayMs;
			// increase delay time by a factor of 1.5 through 2.5
			nextRequestDelayMs *= (int)(1.5 + (Math.random() * 1));

			Logger.warn("Via RecommenderRequester, API Delay increased! Previous delay was " + previousRequestDelay + "ms. New delay is " + nextRequestDelayMs + "ms.");
		}
		else if(nextRequestDelayMs > BASE_REQUEST_DELAY_MS) {
			// if we are nearing the limit and the nextReq is above base delay, cut current in half
			// 	Ensure we don't go below our BASE_REQUEST_DELAY_MS with Math.max
			nextRequestDelayMs = Math.max(nextRequestDelayMs / 2, BASE_REQUEST_DELAY_MS);
		}	
		// update number of requests made
		lastMinuteRequestCount.addAndGet(1);
	}

	public RecommenderSongs getSongsFromSpotify(ArrayList<String> youtubeSongList) {
		String[] songsFromSpotify = getSongArrayFromSpotify(youtubeSongList);

		return new RecommenderSongs(-1, songsFromSpotify);
	}

	public boolean isNearingRateLimit() {
		// divide by 100 because MAX_REQUEST_THRESHOLD is an int as 90%
		return lastMinuteRequestCount.get() >= (MAX_REQUESTS_PER_MIN * MAX_REQUEST_THRESHOLD) / 100;
	}

	public void shutdown() {
		this.scheduler.shutdown();
	}

	private String[] getSongArrayFromSpotify(ArrayList<String> listOfSongs) {
		String[] songs = new String[0];
		int attempts = 0;
		boolean success = false;

		while(!success && attempts < 2) {
			try {
				songs =	generateRecommendationsFromList(listOfSongs);
				success = true;
			}
			catch(IOException ioException) {
				Logger.error(ioException, "IOException occurred while trying to get recommended songs from spotify.");
			}
			catch(SpotifyWebApiException spotifyWebApiException) {
				Logger.error(spotifyWebApiException, "SpotifyWebApiException occurred while trying to get recommended songs from spotify.");
				if(attempts == 0) {
					Logger.error("Going to retry once by refreshing access token.");
					boolean successfullyRefreshed = refreshSpotifyAccessToken();
					if(!successfullyRefreshed) {
						Logger.warn("Spotify token was not refreshed successfully!");
						attempts = 2;
					}
					else {
						Logger.info("Spotify token was refreshed successfully.");
					}
				}
			}
			catch(ParseException parseException) {
				Logger.error(parseException, "ParseException occurred while trying to get recommended songs from spotify.");
			}
			catch(InterruptedException interruptedException) {
				Logger.error(interruptedException, "InterruptedException occurred while trying to get recommended songs from spotify.");
			}
			attempts++;
		}

		return songs;
	}

	private boolean refreshSpotifyAccessToken() {
		boolean success = false;
		try {
			ClientCredentialsRequest clientCredentialsRequest = this.spotifyApi.clientCredentials().build();
			ClientCredentials clientCredentials = clientCredentialsRequest.execute();
			this.spotifyApi.setAccessToken(clientCredentials.getAccessToken());
			Logger.info("Created spotify access token, expires in " + clientCredentials.getExpiresIn());
			success = true;
		}
		catch(IOException ioException) {
			Logger.error(ioException, "IOException occurred while refreshing spotify token.");
		}
		catch(SpotifyWebApiException webApiException) {
			Logger.error(webApiException, "SpotifyWebApiException occurred while refreshing spotify token.");
		}
		catch(ParseException parseException) {
			Logger.error(parseException, "ParseException occurred while refreshing spotify token.");
		}
		return success;
	}

	private String[] generateRecommendationsFromList(List<String> listOfSongs) throws InterruptedException, IOException, ParseException, SpotifyWebApiException {
 		String[] trackIds = getSongIdsFromList(listOfSongs);

		String commaSeparatedTrackIds = String.join(",", trackIds);

		ObjectMapper objectMapper = new ObjectMapper();

		// api call made here so possiblyBackOffApiCallAndUpdateDelay
		possiblyBackOffApiCallAndUpdateDelay();
		String recommendedTracksJson = spotifyApi
			.getRecommendations()
			.limit(5)
			.market(CountryCode.US)
			.seed_tracks(commaSeparatedTrackIds)
			.build()
			.getJson();

		JsonNode jsonNode = objectMapper.readValue(recommendedTracksJson, JsonNode.class).path("tracks");

		List<String> trackTitlesAndArtists = new ArrayList<>();
		if (jsonNode.isArray()) {
			for (JsonNode node : jsonNode) {
				String artist = node.path("artists").get(0).path("name").asText();
				String title = node.path("name").asText();
				trackTitlesAndArtists.add(artist + " - " + title);
			}
		}

		return trackTitlesAndArtists.toArray(new String[0]);
	}

	private String[] getSongIdsFromList(List<String> listOfSongs) throws IOException, ParseException, SpotifyWebApiException, InterruptedException {
		ArrayList<String> foundSongSpotifyIds = new ArrayList<>();
		ObjectMapper objectMapper = new ObjectMapper();
		for(String song : listOfSongs) {
			song = song.replaceAll("[^a-zA-Z0-9 ]", "");
			// request is made here so possiblyBackOffApiCallAndUpdateDelay
			possiblyBackOffApiCallAndUpdateDelay();
			JsonNode jsonNode = objectMapper.readValue(spotifyApi.searchTracks(song).limit(1).market(CountryCode.US).build().getJson(), JsonNode.class).path("tracks").path("items");
			if (jsonNode.isArray() && jsonNode.size() > 0) {
				JsonNode firstItemNode = jsonNode.get(0);
				String trackId = firstItemNode.path("id").asText();
				foundSongSpotifyIds.add(trackId);
			}
		}
		return foundSongSpotifyIds.toArray(new String[0]);
	}
}
