package dev.kmfg.discordbot.services;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.activity.ActivityType;
import org.tinylog.Logger;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import dev.kmfg.sessions.AudioSession;
import dev.kmfg.sessions.SessionManager;

/**
 * {@link ActivityUpdaterService}
 * Updates the bot's activity status with the current playing track IF the bot is ONLY in 1 AudioSession.
 * Activity is updated every ACTIVITY_UPDATE_INTERNVAL_MS
 */
public class ActivityUpdaterService {
    // 60 seconds to milliseconds
    private static final int ACTIVITY_UPDATE_INTERNVAL_MS = 60 * 1000;

    private final DiscordApi discordApi;
    private final SessionManager sessionManager;
    private final ScheduledExecutorService activityUpdateExecutorService;

    private boolean alreadyLoggedCurrentState = false;

    public ActivityUpdaterService(DiscordApi discordApi, SessionManager sessionManager) {
        this.discordApi = discordApi;
        this.sessionManager = sessionManager;
        // setup Executor
        this.activityUpdateExecutorService = Executors.newSingleThreadScheduledExecutor();
        // log that the activity updater service has been initialized
        Logger.info("ActivityUpdaterService has been initialized. (Not running yet)");
    }

    /**
     * Begin interval activity updating
     */
    public void start() {
        // make sure executor will run
        this.updateActivityOnInterval();
        Logger.info("ActivityUpdaterService has started. (Now running)");
    }

    /**
     * sets up the executor service to run the activity update method
     */
    protected void updateActivityOnInterval() {
        this.activityUpdateExecutorService
            .scheduleAtFixedRate(
                    // on interval run the updateActivity method
                    this::updateActivity,
                    // the start delay is
                    ACTIVITY_UPDATE_INTERNVAL_MS,
                    // the delay after start delay is
                    ACTIVITY_UPDATE_INTERNVAL_MS,
                    // the units of these delays are
                    TimeUnit.MILLISECONDS
                    );
    }

    protected void possiblyLogSkip() {
        if(!this.alreadyLoggedCurrentState) {
            String mainTitleReason = "ActivityUpdaterService skipped updating because the only audio";
            String furtherReasonInfo = "\n\tSessionManager claims a total of ";

            StringBuilder loggaInfa = new StringBuilder()
                .append(mainTitleReason)
                .append(furtherReasonInfo)
                .append(this.sessionManager.getTotalSessionCount())
                .append(" sessions.");

            Logger.info(loggaInfa.toString());

            // Additionally clear the current activity
            this.discordApi.unsetActivity();


            this.alreadyLoggedCurrentState = true;
        }
    }

    /**
     * Updates the activity to the current track.
     * Will do nothing if total AudioSessions are > 1 OR < 1
     */
    protected void updateActivity() {
        // get the possible only audio session from the manager
        Optional<AudioSession> onlyAudioSession = this.sessionManager.getOnlyAudioSession();

        // if we dont have anything, there are either 0 or greater than 1 sessions
        if(onlyAudioSession.isEmpty()) {
            possiblyLogSkip();
            // early return
            return;
        }

        AudioSession audioSession = onlyAudioSession.get();
        AudioTrack currentTrack = audioSession.getLavaSource().getCurrentPlayingAudioTrack();

        // if there is no current playing track
        if(currentTrack == null) {
            Logger.info("ActivityUpdaterService skipped updating because the current track was null.\n\tHowever, the AudioSession was not.");
            Logger.info(new StringBuilder().append("\n\t").append(audioSession.toString()));
            return;
        }

        // max title and/or author length should be...
        int MAX_INFO_LENGTH = 30;
        String currentTrackTitle = currentTrack.getInfo().title.length() > MAX_INFO_LENGTH ?
            currentTrack.getInfo().title.substring(0, MAX_INFO_LENGTH) : currentTrack.getInfo().title;
        String currentTrackAuthor = currentTrack.getInfo().author.length() > MAX_INFO_LENGTH ?
            currentTrack.getInfo().author.substring(0, MAX_INFO_LENGTH) : currentTrack.getInfo().author;
        StringBuilder acitivtyNameBuilder = new StringBuilder()
            .append(currentTrackTitle)
            .append(" by ")
            .append(currentTrackAuthor);

        Logger.info(
                new StringBuilder()
                .append("Activity updated to \n\t")
                .append(acitivtyNameBuilder.toString())
                .toString()
                );

        this.alreadyLoggedCurrentState = false;

        this.discordApi.updateActivity(ActivityType.LISTENING, acitivtyNameBuilder.toString());
    }
}
