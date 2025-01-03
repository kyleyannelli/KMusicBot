package dev.kmfg.musicbot.database.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "used_commands")
public class UsedCommand extends BaseKMusicTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "name")
    private String name;

    @ManyToOne
    @JoinColumn(name = "discord_guild_id")
    private DiscordGuild discordGuild;

    @ManyToOne
    @JoinColumn(name = "used_by_discord_user_id")
    private DiscordUser usedByDiscordUser;

    @Column(name = "times_used")
    private int timesUsed;

    public UsedCommand() {

    }

    public UsedCommand(String commandName, DiscordGuild discordGuild, DiscordUser discordUser) {
        this.name = commandName;
        this.discordGuild = discordGuild;
        this.usedByDiscordUser = discordUser;
    }

    public void incTimesUsed() {
        this.timesUsed++;
    }

    public int getTimesUsed() {
        return this.timesUsed;
    }

    public DiscordGuild getGuild() {
        return this.discordGuild;
    }

    public DiscordUser usedBy() {
        return this.usedByDiscordUser;
    }

    public String getName() {
        return this.name;
    }
}
