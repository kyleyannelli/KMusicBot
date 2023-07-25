package Helpers;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;

public class QueueResult {
	private final boolean success;
	private final List<AudioTrack> queuedTracks;

	public QueueResult(boolean success, List<AudioTrack> queuedTracks) {
		this.success = success;
		this.queuedTracks = queuedTracks;
	}

	public boolean isSuccess() {
		return success;
	}

	public List<AudioTrack> getQueuedTracks() {
		return queuedTracks;
	}
}
