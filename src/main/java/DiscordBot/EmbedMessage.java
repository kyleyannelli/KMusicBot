package DiscordBot;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

public class EmbedMessage {
	private final SlashCommandInteraction slashCommandInteraction;
	private final CompletableFuture<InteractionOriginalResponseUpdater> respondLater;
	private String content;
	private String title;
	private Color color;

	public EmbedMessage(SlashCommandInteraction slashCommandInteraction, CompletableFuture<InteractionOriginalResponseUpdater> respondLater) {
		this.slashCommandInteraction = slashCommandInteraction;
		this.respondLater = respondLater;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void send() {
		User requestingUser = slashCommandInteraction.getUser();

		EmbedBuilder embedBuilder = new EmbedBuilder()
			.setTitle(this.title)
			.setAuthor(requestingUser)
			.addField("", content)
			.setColor(this.color == null ? Color.BLUE : this.color);

		this.respondLater.thenAccept(acceptance -> {
			acceptance.addEmbed(embedBuilder);
			acceptance.update();	
		});
	}
}
