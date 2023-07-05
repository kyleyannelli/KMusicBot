package SpotifyApi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovisionaries.i18n.CountryCode;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;

public class RadioSongsHandler {
	public static String[] getSongIdsFromList(SpotifyApi spotifyApi, List<String> listOfSongs) throws IOException, ParseException, SpotifyWebApiException, InterruptedException {
		ArrayList<String> foundSongSpotifyIds = new ArrayList<>();
		ObjectMapper objectMapper = new ObjectMapper();
		for(String song : listOfSongs) {
			Thread.sleep(1000);
			JsonNode jsonNode = objectMapper.readValue(spotifyApi.searchTracks(song).limit(1).market(CountryCode.US).build().getJson(), JsonNode.class).path("tracks").path("items");
			if (jsonNode.isArray() && jsonNode.size() > 0) {
				JsonNode firstItemNode = jsonNode.get(0);
				String trackId = firstItemNode.path("id").asText();
				foundSongSpotifyIds.add(trackId);
			}
		}
		return foundSongSpotifyIds.toArray(new String[0]);
	}

	public static String[] generateRecommendationsFromList(SpotifyApi spotifyApi, List<String> listOfSongs) throws IOException, ParseException, SpotifyWebApiException {
		String[] trackIds;
		try {
			trackIds = getSongIdsFromList(spotifyApi, listOfSongs);
		}
		catch(Exception e) {
			System.out.println("Error Getting Song IDs: " + e.getMessage());
			return null;
		}

		String commaSeparatedTrackIds = String.join(",", trackIds);
		ObjectMapper objectMapper = new ObjectMapper();
		String recommendedTracksJson = spotifyApi
				.getRecommendations()
				.limit(10)
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

	public static void main(String[] args) throws IOException, ParseException, SpotifyWebApiException, InterruptedException {
		ArrayList<String> songsWhichWereQueued = new ArrayList<>();	
		songsWhichWereQueued.add("Healing COMPUTER DATA");
		songsWhichWereQueued.add("Nova MPH");
		songsWhichWereQueued.add("Calico 1tbsp");
		songsWhichWereQueued.add("Love reigns mall grab");

		SpotifyApi api = ClientCreate.clientCredentials_Sync();

		Arrays.stream(generateRecommendationsFromList(api, songsWhichWereQueued)).forEach(System.out::println);
	}
}
