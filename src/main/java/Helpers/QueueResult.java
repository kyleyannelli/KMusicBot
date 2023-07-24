package Helpers;

import java.util.List;

public class QueueResult {
	private final boolean success;
	private final List<String> queuedTracks;

	public QueueResult(boolean success, List<String> queuedTracks) {
		this.success = success;
		this.queuedTracks = queuedTracks;
	}

	public boolean isSuccess() {
		return success;
	}

	public List<String> getQueuedTracks() {
		return queuedTracks;
	}
}
