package dev.kmfg.discordbot;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SelectMenuInteraction;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

public class EmbedMessage {
	private static final String YOUTUBE_THUMBNAIL_BEGIN_URI = "https://img.youtube.com/vi/";
	private static final String YOUTUBE_THUMBNAIL_END_URI = "/0.jpg";

	private final SlashCommandInteraction slashCommandInteraction;
	private final SelectMenuInteraction selectMenuInteraction;
	private final CompletableFuture<InteractionOriginalResponseUpdater> respondLater;

	private String content;
	private String title;
	private String forcedTitle;
	private String author;
	private String youtubeTitle;
	private String youtubeUri;
	private String fullYoutubeThumbnailUri;
	private Color color;
	private boolean isQueue;

	public EmbedMessage(SelectMenuInteraction selectMenuInteraction, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
		this.respondLater = respondLater;
		this.slashCommandInteraction = null;
		this.selectMenuInteraction = selectMenuInteraction;
	}

	public EmbedMessage(SlashCommandCreateEvent slashCommandCreateEvent, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
		this.slashCommandInteraction = slashCommandCreateEvent.getSlashCommandInteraction();
		this.selectMenuInteraction = null;
		this.respondLater = respondLater;
		this.isQueue = false;
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

		return embedBuilder;
	}

	public void send() {
		User requestingUser = slashCommandInteraction == null ? selectMenuInteraction.getUser() : slashCommandInteraction.getUser();

		EmbedBuilder embedBuilder = generateEmbedBuilder(requestingUser);

 		this.respondLater.thenAccept(acceptance -> {
			acceptance.addEmbed(embedBuilder);
			acceptance.update();
		});
	}

	public CompletableFuture<InteractionOriginalResponseUpdater> getRespondLater() {
		return respondLater;
	}
}
