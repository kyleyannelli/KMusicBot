package Helpers;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;

public class QueueResult {
	private final boolean success;
	private final boolean willPlayNow;
	private final List<AudioTrack> queuedTracks;

	public QueueResult(boolean success, boolean willPlayNow, List<AudioTrack> queuedTracks) {
		this.success = success;
		this.queuedTracks = queuedTracks;
		this.willPlayNow = willPlayNow;
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
}
