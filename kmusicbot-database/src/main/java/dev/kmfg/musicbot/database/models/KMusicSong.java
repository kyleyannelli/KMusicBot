package dev.kmfg.musicbot.database.models;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.google.gson.annotations.Expose;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "kmusic_songs")
public class KMusicSong extends BaseKMusicTable {
    public static final int MAX_AUTHOR_LENGTH = 50;
    public static final int MAX_TITLE_LENGTH = 75;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Expose
    private int id;

    @Column(name = "youtube_url")
    @Expose
    private String youtubeUrl;

    @Column(name = "author")
    @Expose
    private String author;

    @Column(name = "title")
    @Expose
    private String title;

    @OneToMany(mappedBy = "kmusicSong")
    Set<TrackedSong> trackedSongs = new HashSet<>();

    public KMusicSong() {

    }

    public KMusicSong(String youtubeUrl) {
        this.youtubeUrl = youtubeUrl;
    }

    public KMusicSong(String youtubeUrl, String author, String title) {
        this.youtubeUrl = youtubeUrl;

        if (author.length() > MAX_AUTHOR_LENGTH) {
            this.author = author.substring(0, MAX_AUTHOR_LENGTH);
        } else {
            this.author = author;
        }

        if (title.length() > MAX_TITLE_LENGTH) {
            this.title = title.substring(0, MAX_TITLE_LENGTH);
        } else {
            this.title = title;
        }
    }

    public int getId() {
        return this.id;
    }

    public String getYoutubeUrl() {
        return this.youtubeUrl;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Optional<String> getAuthor() {
        return Optional.ofNullable(this.author);
    }

    public Optional<String> getTitle() {
        return Optional.ofNullable(this.title);
    }
}
