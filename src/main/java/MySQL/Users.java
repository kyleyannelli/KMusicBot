package MySQL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Users {
    private long discordId;
    private long songId;
    private long timeSpent;
    private long serverDiscordId;

    public Users(long discordId, long serverDiscordId, long songId, long timeSpent) {
        this.discordId = discordId;
        this.songId = songId;
        this.timeSpent = timeSpent;
        this.serverDiscordId = serverDiscordId;
    }

    public long save(String dB) {
        Connection conn;
        try {
            // get connection
            conn = CustomConnection.getConnection("DISCORD_" + dB);
        }
        catch (Exception e) {
            System.out.println("Error connecting to database");
            e.printStackTrace();
            return -1;
        }
        PreparedStatement stmt;
        ResultSet rs;
        // save song
        try {
            String sql = "INSERT INTO users (discord_id, server_discord_id, song_id, time_spent) VALUES (?, ?, ?, ?)";
            stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setLong(1, discordId);
            stmt.setLong(2, serverDiscordId);
            stmt.setLong(3, songId);
            stmt.setLong(4, timeSpent);
        }
        catch (Exception e) {
            System.out.println("Error saving song");
            e.printStackTrace();
            return -1;
        }

        // close connection
        try {
            // get user id where server_discord_id = serverDiscordId and discord_id = discordId and song_id = songId
            String sql = "SELECT discord_id FROM users WHERE discord_id = ? AND server_discord_id = ? AND song_id = ?";
            stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setLong(1, discordId);
            stmt.setLong(2, serverDiscordId);
            stmt.setLong(3, songId);
            long id = stmt.getGeneratedKeys().getLong(1);
            conn.close();
            return id;
        }
        catch (Exception e) {
            System.out.println("Error closing connection");
            e.printStackTrace();
            return -1;
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
                    + "    PRIMARY KEY (discord_id, server_discord_id, song_id)\n"
                    + ")";
            PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
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

    public static void addToTimeSpent(long dB, long discordId, long songId, long timeSpent) {
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
        PreparedStatement stmt;
        // save song
        try {
            String sql = "UPDATE users SET time_spent = time_spent + ? WHERE discord_id = ? AND server_discord_id = ? AND song_id = ?";
            stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setLong(1, timeSpent);
            stmt.setLong(2, discordId);
            stmt.setLong(3, dB);
            stmt.setLong(4, songId);
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
}
