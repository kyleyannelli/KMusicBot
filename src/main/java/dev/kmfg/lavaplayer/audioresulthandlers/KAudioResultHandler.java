package dev.kmfg.lavaplayer.audioresulthandlers;

import dev.kmfg.exceptions.AlreadyAccessedException;
import dev.kmfg.helpers.SingleUse;
import dev.kmfg.lavaplayer.ProperTrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.ArrayList;

/**
 * An abstract class which loads the result of a query from an AudioSource, such as Youtube.
 * This holds SingleUse wrappers to allow the user to access values with a guaranteed lifetime, even though the class itself may live much longer.
 */
public abstract class KAudioResultHandler implements AudioLoadResultHandler {
    protected ProperTrackScheduler trackScheduler;
    protected SingleUse<Boolean> isSuccess;
    protected SingleUse<ArrayList<AudioTrack>> lastLoadedTracks;
    protected boolean deprioritizeQueue;
    protected boolean playNext;

    public KAudioResultHandler(ProperTrackScheduler trackScheduler) {
        this.trackScheduler = trackScheduler;
        // by default do not deprioritize
        this.deprioritizeQueue = false;
        this.playNext = false;
    }

    /**
     * Get the tracks which were last loaded.
     * Since KAudioResultHandlers (and its children) may have a long lifetime, this value can only be accessed once, until it is updated.
     * The above behavior is set as such so you can guarantee new access is a new set of data.
     * @return ArrayList<AudioTrack>
     * @throws AlreadyAccessedException If the data was already accessed, this exception will be thrown.
     */
    public ArrayList<AudioTrack> getLastLoadedTracks() throws AlreadyAccessedException {
        return lastLoadedTracks.get();
    }

    /**
     * Returns true if the AudioTrack or playlist was successfully loaded. False otherwise.
     * This uses a SingleUse wrapper to make sure the value can only be accessed once, until it is updated.
     * This guarantees new access is new data.
     * @return boolean
     * @throws AlreadyAccessedException If the data was already accessed, this exception will be thrown.
     */
    public boolean getIsSuccess() throws AlreadyAccessedException {
        return isSuccess.get();
    }

    /**
     * Sets the handler to queue any tracks into the deprioritized queue.
     * This queue is currently used for songs added by the RecommenderProcessor, although, it could have other uses.
     */
    public void deprioritizeQueue() {
        this.deprioritizeQueue = true;
    }

    /**
     * Sets the handler to queue any tracks into the prioritized queue.
     * This queue is currently used for songs added by the end user, although, it could have other uses.
     */
    public void prioritizeQueue() {
        this.deprioritizeQueue = false;
    }

    /**
     * Set whether the song which gets added next goes to the head of the priority queue.
     * @param playNext pass in a true value to play the song next in the priority queue, false to send the song at the tail of the queue.
     */
    public void setPlayNext(boolean playNext) {
        this.playNext = playNext;
    }
}
