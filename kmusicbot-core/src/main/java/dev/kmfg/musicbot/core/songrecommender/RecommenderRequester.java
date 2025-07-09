package dev.kmfg.musicbot.core.songrecommender;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import org.tinylog.Logger;

/**
 * Provides the ability to call an API without worrying about rate limiting.
 */
public class RecommenderRequester {
    private final int MAX_REQUESTS_PER_MIN = 10;
    // Start backing off when we are at 90% of the MAX_REQUESTS_PER_MIN
    private final int MAX_REQUEST_THRESHOLD = 90;
    // Delay to API in milliseconds
    private final int BASE_REQUEST_DELAY_MS = 1500;
    private int nextRequestDelayMs = BASE_REQUEST_DELAY_MS;

    private final AtomicInteger lastMinuteRequestCount;
    private final ScheduledExecutorService scheduler;
    private final RecommenderThirdParty recommenderThirdParty;

    public RecommenderRequester(RecommenderThirdParty recommenderThirdParty) {
        this.recommenderThirdParty = recommenderThirdParty;

        this.lastMinuteRequestCount = new AtomicInteger(0);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        // every minute reset lastMinuteRequestCount to 0
        this.scheduler.scheduleAtFixedRate(() -> lastMinuteRequestCount.set(0), 1, 1, TimeUnit.MINUTES);
    }

    public void backoff() {
        try {
            Thread.sleep(nextRequestDelayMs);
        } catch (InterruptedException iE) {
            // let it be interruptted
            Thread.currentThread().interrupt();
        }
        if (isNearingRateLimit()) {
            int previousRequestDelay = nextRequestDelayMs;
            // increase delay time by a factor of 1.5 through 2.5
            nextRequestDelayMs *= (int) (1.5 + (Math.random() * 1));

            Logger.warn("Via RecommenderRequester, API Delay increased! Previous delay was " + previousRequestDelay
                    + "ms. New delay is " + nextRequestDelayMs + "ms.");
        } else if (nextRequestDelayMs > BASE_REQUEST_DELAY_MS) {
            // if we are nearing the limit and the nextReq is above base delay, cut current
            // in half
            // Ensure we don't go below our BASE_REQUEST_DELAY_MS with Math.max
            nextRequestDelayMs = Math.max(nextRequestDelayMs / 2, BASE_REQUEST_DELAY_MS);
        }
        // update number of requests made
        lastMinuteRequestCount.addAndGet(1);
    }

    public String[] getSongsFromThirdParty(ArrayList<AudioTrack> queuedSongs) {
        backoff();
        return recommenderThirdParty.recommend(queuedSongs);
    }

    public boolean isNearingRateLimit() {
        // divide by 100 because MAX_REQUEST_THRESHOLD is an int as 90%
        return lastMinuteRequestCount.get() >= (MAX_REQUESTS_PER_MIN * MAX_REQUEST_THRESHOLD) / 100;
    }

    public void shutdown() {
        this.scheduler.shutdown();
    }
}