package dev.kmfg.musicbot.core.songrecommender.youtubeapi;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.kmfg.musicbot.core.songrecommender.RecommenderThirdParty;
import org.tinylog.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecommenderYoutubeScraper implements RecommenderThirdParty {
    private static final String SCRIPT_PATH = "yt.related.sh";
    private static final String VIDEO_ID_REGEX = "[?&]v=([^&#]*)";
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(VIDEO_ID_REGEX);

    @Override
    public String[] recommend(AudioTrack starterSong) {
        final String youtubeUri = starterSong.getInfo().uri;
        final Matcher videoIdMatcher = VIDEO_ID_PATTERN.matcher(youtubeUri);

        if(!videoIdMatcher.find()) {
            Logger.warn("Failed to find recommendations for %s", youtubeUri);
            return new String[0];
        }

        final String videoId = videoIdMatcher.group(1);
        return runRecommendationScript(videoId);
    }

    @Override
    public String[] recommend(AudioTrack[] songs) {
        final List<String> recommendations = new ArrayList<>();
        for(AudioTrack song : songs) {
            Collections.addAll(
                    recommendations,
                    recommend(song)
            );
        }
        return recommendations.toArray(new String[0]);
    }

    @Override
    public String[] recommend(List<AudioTrack> songs) {
        final List<String> recommendations = new ArrayList<>();
        for(AudioTrack song : songs) {
            Collections.addAll(
                    recommendations,
                    recommend(song)
            );
        }
        return recommendations.toArray(new String[0]);
    }

    private String[] runRecommendationScript(final String videoId) {
        final Optional<Path> tempScriptPathOpt = loadScriptToTemp();
        if(tempScriptPathOpt.isEmpty()) {
            Logger.warn("Temp script path not returned from loadScriptToTemp method for %s.", videoId);
            return new String[0];
        }

        final Path tempScriptPath = tempScriptPathOpt.get();
        final Optional<String[]> recommendedUrisOpt = runScript(
                tempScriptPath.toAbsolutePath().toString(),
                videoId
        );
        if(recommendedUrisOpt.isEmpty()) {
            Logger.warn("No uris returned for %s | %s", tempScriptPath.toAbsolutePath().toString(), videoId);
            return new String[0];
        }

        return recommendedUrisOpt.get();
    }

    private Optional<Path> loadScriptToTemp() {
        try(InputStream in = RecommenderYoutubeScraper.class.getClassLoader().getResourceAsStream(SCRIPT_PATH)) {
            if(in == null) {
                Logger.warn("%s was not found!", SCRIPT_PATH);
                return Optional.empty();
            }

            final Path tempScript = Files.createTempFile(SCRIPT_PATH, ".temp.sh");
            Files.copy(in, tempScript, StandardCopyOption.REPLACE_EXISTING);
            tempScript.toFile().setExecutable(true);

            return Optional.of(tempScript);
        } catch(IOException e) {
            Logger.error("Failed to open %s with %s", SCRIPT_PATH, e.getCause());
            return Optional.empty();
        }
    }

    private Optional<String[]> runScript(final String scriptPath, final String videoId) {
        final ProcessBuilder pb = new ProcessBuilder(scriptPath, videoId);
        pb.redirectErrorStream(true);

        try {
            final Process process = pb.start();
            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            process.getInputStream()
                    )
            );

            final List<String> urls = new ArrayList<>();
            String url;
            while((url = reader.readLine()) != null) {
                urls.add(url);
            }
            process.waitFor();

            return Optional.of(urls.toArray(new String[0]));
        } catch(IOException e) {
            Logger.error("Process for %s failed %s", scriptPath, e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }
}
