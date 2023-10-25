package dev.kmfg.database.repositories.interfaces;

import java.util.Optional;

import dev.kmfg.database.models.DiscordUser;

public interface DiscordUserRepository {
	Optional<DiscordUser> findByDiscordId(long discordId);
	Optional<DiscordUser> findByDiscordUsername(String discordUsername);
	Optional<DiscordUser> save(DiscordUser discordUser);
}
