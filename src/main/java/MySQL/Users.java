package MySQL;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class Users {
    private long discordId;
    private long songId;
    private long timeSpent;
    private long serverDiscordId;

    public Users(long discordId, long serverDiscordId, long songId, long timeSpent) {
        this.discordId = discordId;
        this.songId = songId;
        this.timeSpent = timeSpent;
    }

    public void save(String dB) {
        Connection conn;
        try {
            // get connection
            conn = CustomConnection.getConnection("DISCORD_" + dB);
        }
        catch (Exception e) {
            System.out.println("Error connecting to database");
            e.printStackTrace();
            return;
        }

        // save song
        try {
            String sql = "INSERT INTO users (discord_id, server_discord_id, song_id, time_spent) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, discordId);
            stmt.setLong(2, serverDiscordId);
            stmt.setLong(3, songId);
            stmt.setLong(4, timeSpent);
            stmt.executeUpdate();
        }
        catch (Exception e) {
            System.out.println("Error saving song");
            e.printStackTrace();
            return;
        }

        // close connection
        try {
            conn.close();
        }
        catch (Exception e) {
            System.out.println("Error closing connection");
            e.printStackTrace();
            return;
        }
    }

    public static void setupTable(String dB) {
        Connection conn;
        try {
            // get connection
            conn = CustomConnection.getConnection("DISCORD_" + dB);
        }
        catch (Exception e) {
            System.out.println("Error connecting to database");
            e.printStackTrace();
            return;
        }

        // create table
        try {
            String sql = "CREATE TABLE IF NOT EXISTS users (\n"
                    + "    discord_id BIGINT NOT NULL,\n"
                    + "    server_discord_id BIGINT NOT NULL,\n"
                    + "    song_id BIGINT NOT NULL,\n"
                    + "    time_spent BIGINT NULL,\n"
                    + "    PRIMARY KEY (discord_id, song_id)\n"
                    + ")";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.executeUpdate();
        }
        catch (Exception e) {
            System.out.println("Error creating table");
            e.printStackTrace();
            return;
        }

        // close connection
        try {
            conn.close();
        }
        catch (Exception e) {
            System.out.println("Error closing connection");
            e.printStackTrace();
            return;
        }
    }
}
