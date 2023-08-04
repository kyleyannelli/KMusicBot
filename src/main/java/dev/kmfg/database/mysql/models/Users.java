package dev.kmfg.database.mysql.models;

import dev.kmfg.database.CustomConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
        // save song
        try {
            String sql = "INSERT INTO users (discord_id, server_discord_id, song_id, time_spent) VALUES (?, ?, ?, ?)";
            assert conn != null;
            assertAndExecute(songId, serverDiscordId, timeSpent, discordId, conn, sql);
        }
        catch (Exception e) {
            // close connection
            try {
                // get user id where server_discord_id = serverDiscordId and discord_id = discordId and song_id = songId
                return executeWhere(conn);
            }
            catch (Exception e2) {
                System.out.println("Error closing connection");
                e.printStackTrace();
                e2.printStackTrace();
                return -1;
            }
        }

        // close connection
        try {
            // get user id where server_discord_id = serverDiscordId and discord_id = discordId and song_id = songId
            return executeWhere(conn);
        }
        catch (Exception e) {
            System.out.println("Error closing connection");
            e.printStackTrace();
            return -1;
        }
    }

    private long executeWhere(Connection conn) throws SQLException {
        PreparedStatement stmt;
        String sql = "SELECT discord_id FROM users WHERE discord_id = ? AND server_discord_id = ? AND song_id = ?";
        stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
        stmt.setLong(1, discordId);
        stmt.setLong(2, serverDiscordId);
        stmt.setLong(3, songId);
        ResultSet rs = stmt.executeQuery();
        rs.next();
        long id = rs.getLong(1);
        conn.close();
        return id;
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
            assert conn != null;
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
        // save song
        try {
            String sql = "UPDATE users SET time_spent = time_spent + ? WHERE discord_id = ? AND server_discord_id = ? AND song_id = ?";
            assert conn != null;
            assertAndExecute(dB, discordId, songId, timeSpent, conn, sql);
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
        }
    }

    private static void assertAndExecute(long dB, long discordId, long songId, long timeSpent, Connection conn, String sql) throws SQLException {
        PreparedStatement stmt;
        assert conn != null;
        stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
        stmt.setLong(1, timeSpent);
        stmt.setLong(2, discordId);
        stmt.setLong(3, dB);
        stmt.setLong(4, songId);
        stmt.executeUpdate();
    }
}
