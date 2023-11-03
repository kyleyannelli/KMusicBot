package dev.kmfg.musicbot.core.util.sessions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;

public class QueueResult {
	private final boolean success;
	private final boolean willPlayNow;
	private final List<AudioTrack> queuedTracks;
	private final AudioTrack queuedTrack;

	public QueueResult(boolean success, boolean willPlayNow, List<AudioTrack> queuedTracks) {
		this.success = success;
		this.queuedTracks = queuedTracks;
		this.willPlayNow = willPlayNow;
		this.queuedTrack = null;
	}

	public QueueResult(boolean success, boolean willPlayNow, AudioTrack queuedTrack) {
		this.success = success;
		this.queuedTracks = null;
		this.willPlayNow = willPlayNow;
		this.queuedTrack = queuedTrack;
	}

	public boolean isSuccess() {
		return this.success;
	}

	public boolean willPlayNow() {
		return this.willPlayNow;
	}

	public List<AudioTrack> getQueuedTracks() {
		return this.queuedTracks;
	}

	public AudioTrack getQueueTrack() {
		return queuedTrack;
	}
}
