package DiscordBot;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

public class EmbedMessage {
	private static final String YOUTUBE_THUMBNAIL_BEGIN_URI = "https://img.youtube.com/vi/";
	private static final String YOUTUBE_THUMBNAIL_END_URI = "/0.jpg";

	private final SlashCommandInteraction slashCommandInteraction;
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

	public EmbedMessage(SlashCommandInteraction slashCommandInteraction, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
		this.slashCommandInteraction = slashCommandInteraction;
		this.respondLater = respondLater;
		this.isQueue = false;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setIsQueue(boolean isQueue) {
		this.isQueue = isQueue;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void setForcedTitle(String forcedTitle) {
		this.forcedTitle = forcedTitle;
	}
	
	public void setYoutubeUri(String youtubeUri) {
		this.youtubeUri = youtubeUri;
	}

	public void setupAudioTrack(AudioTrack audioTrack) {
		this.youtubeTitle = audioTrack.getInfo().title;
		this.author = audioTrack.getInfo().author;
		this.youtubeUri = audioTrack.getInfo().uri;

		String identifier = audioTrack.getInfo().identifier;
		this.fullYoutubeThumbnailUri = YOUTUBE_THUMBNAIL_BEGIN_URI + identifier + YOUTUBE_THUMBNAIL_END_URI;
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
		User requestingUser = slashCommandInteraction.getUser();

		EmbedBuilder embedBuilder = generateEmbedBuilder(requestingUser);

 		this.respondLater.thenAccept(acceptance -> {
			acceptance.addEmbed(embedBuilder);
			acceptance.update();
		});
	}
}
