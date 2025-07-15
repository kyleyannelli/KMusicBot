package dev.kmfg.musicbot.database.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;

@Entity
@Table(name = "playlists")
public class Playlist extends BaseKMusicTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "discord_guild_id")
    private DiscordGuild guild;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "kmusic_songs_playlists",
            joinColumns = @JoinColumn(name = "playlist_id"),
            inverseJoinColumns = @JoinColumn(name = "kmusic_song_id")
    )
    private Set<KMusicSong> songs;

    public Playlist setName(String name) {
        this.name = name.replace(':', ' ');
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
