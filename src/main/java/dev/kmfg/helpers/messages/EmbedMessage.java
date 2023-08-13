package dev.kmfg.helpers.messages;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.callback.ComponentInteractionOriginalMessageUpdater;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

public class EmbedMessage {
	private static final String YOUTUBE_THUMBNAIL_BEGIN_URI = "https://img.youtube.com/vi/";
	private static final String YOUTUBE_THUMBNAIL_END_URI = "/0.jpg";
	private final User requestingUser;
	private final CompletableFuture<InteractionOriginalResponseUpdater> respondLater;
	private final ComponentInteractionOriginalMessageUpdater originalMessageUpdater;
	private String content;
	private String title;
	private String forcedTitle;
	private String author;
	private String forcedContent;
	private String youtubeTitle;
	private String youtubeUri;
	private String fullYoutubeThumbnailUri;
	private Color color;
	private boolean isQueue;

	public EmbedMessage(User requestingUser, ComponentInteractionOriginalMessageUpdater originalMessageUpdater) {
		this.respondLater = null;
		this.originalMessageUpdater = originalMessageUpdater;
		this.requestingUser = requestingUser;
	}

	public EmbedMessage(User requestingUser, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
		this.respondLater = respondLater;
		this.originalMessageUpdater = null;
		this.requestingUser = requestingUser;
	}

	public EmbedMessage setContent(String content) {
		this.content = content;
		return this;
	}

	public EmbedMessage setTitle(String title) {
		this.title = title;
		return this;
	}

	public EmbedMessage setIsQueue(boolean isQueue) {
		this.isQueue = isQueue;
		return this;
	}

	public EmbedMessage setColor(Color color) {
		this.color = color;
		return this;
	}

	public EmbedMessage setForcedTitle(String forcedTitle) {
		this.forcedTitle = forcedTitle;
		return this;
	}
	
	public EmbedMessage setYoutubeUri(String youtubeUri) {
		this.youtubeUri = youtubeUri;
		return this;
	}

	public void setForcedContent(String content) {
		this.forcedContent = content;
	}

	public EmbedMessage setupAudioTrack(AudioTrack audioTrack) {
		this.youtubeTitle = audioTrack.getInfo().title;
		this.author = audioTrack.getInfo().author;
		this.youtubeUri = audioTrack.getInfo().uri;

		String identifier = audioTrack.getInfo().identifier;
		this.fullYoutubeThumbnailUri = YOUTUBE_THUMBNAIL_BEGIN_URI + identifier + YOUTUBE_THUMBNAIL_END_URI;
		return this;
	}

	public EmbedBuilder generateEmbedBuilder(User requestingUser) {
		EmbedBuilder embedBuilder;
		if(youtubeTitle == null || fullYoutubeThumbnailUri == null || author == null) {
			embedBuilder = new EmbedBuilder()
				.setTitle(this.title == null ? "" : this.title)
				.setAuthor(requestingUser)
				.addField("", content)
				.setColor(this.color == null ? Color.BLUE : this.color);
		}
		else if(isQueue) {
			embedBuilder = new EmbedBuilder()
				.setTitle("Queued:")
				.setAuthor(requestingUser)
				.setThumbnail(fullYoutubeThumbnailUri == null ? "" : fullYoutubeThumbnailUri)
				.setColor(Color.BLACK)
				.setUrl(this.youtubeUri == null ? "" : this.youtubeUri)
				.addField(this.youtubeTitle == null ? "" : this.youtubeTitle, "")
				.addField("", author == null ? "" : author);
		}
		else {
			embedBuilder = new EmbedBuilder()
				.setTitle(youtubeTitle == null ? this.title : youtubeTitle)
				.setAuthor(requestingUser)
				.setImage(fullYoutubeThumbnailUri == null ? "" : fullYoutubeThumbnailUri)
				.setColor(this.color == null ? Color.BLUE : this.color)
				.setUrl(this.youtubeUri == null ? "" : this.youtubeUri)
				.addField("", author == null ? "" : author);
		}

		if(forcedTitle != null) embedBuilder.setTitle(forcedTitle);
		if(forcedContent != null) {
			embedBuilder.removeAllFields();
			embedBuilder.addField("", forcedContent);
		}

		return embedBuilder;
	}

	public void send() {
		EmbedBuilder embedBuilder = generateEmbedBuilder(this.requestingUser);

		if(this.originalMessageUpdater == null) {
			this.respondLater.thenAccept(acceptance -> {
				acceptance.addEmbed(embedBuilder);
				acceptance.update();
			});
		}
		else {
			this.originalMessageUpdater.addEmbed(embedBuilder);
			this.originalMessageUpdater.update();
		}
	}

	public CompletableFuture<InteractionOriginalResponseUpdater> getRespondLater() {
		return respondLater;
	}
}
