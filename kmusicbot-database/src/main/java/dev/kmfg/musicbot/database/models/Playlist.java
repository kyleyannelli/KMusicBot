package dev.kmfg.musicbot.database.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "playlist")
public class Playlist extends BaseKMusicTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "name")
    private String name;

    @ManyToOne
    @JoinColumn(name = "discord_guild_id")
    private DiscordGuild guild;

    @ManyToMany
    @JoinTable(name = "kmusic_songs_playlists")
    private Set<KMusicSong> songs;

    public Playlist setName(String name) {
        this.name = name;
        return this;
    }

    public Playlist addSong(KMusicSong song) {
        songs.add(song);
        return this;
    }

    public Playlist setGuild(DiscordGuild guild) {
        this.guild = guild;
        return this;
    }

    public String getName() {
        return name;
    }

    public DiscordGuild getGuild() {
        return guild;
    }

    public int getId() {
        return id;
    }

    public List<KMusicSong> getSongs() {
        final ArrayList<KMusicSong> songs = new ArrayList<>(this.songs.size());
        for (Object kms : this.songs.toArray()) {
            songs.add((KMusicSong) kms);
        }
        return songs;
    }
}
