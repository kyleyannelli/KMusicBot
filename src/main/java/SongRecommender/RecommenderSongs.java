package SongRecommender;

import java.util.ArrayList;

public class RecommenderSongs {
	private final int TOTAL_REQUESTS_MADE;
	private final String[] recommendedSongsArray;

	public RecommenderSongs(int totalRequestsMade, ArrayList<String> recommendedSongsList) {
		this.TOTAL_REQUESTS_MADE = totalRequestsMade;
		this.recommendedSongsArray = recommendedSongsList.toArray(new String[0]);
	}

	public RecommenderSongs(int totalRequestsMade, String[] recommendedSongsArray) {
		this.TOTAL_REQUESTS_MADE = totalRequestsMade;
		this.recommendedSongsArray = recommendedSongsArray;
	}

	public int getTotalRequestsMade() {
		return TOTAL_REQUESTS_MADE;
	}

	public String[] getRecommendedSongs() {
		return recommendedSongsArray;
	}
}
