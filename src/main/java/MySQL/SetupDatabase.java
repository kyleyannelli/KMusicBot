package MySQL;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class SetupDatabase {
    public static void setup(String dB) {
        // Create database
        createDatabase(dB);
        // Create tables
        SetupTables.setup(dB);
    }

    private static void createDatabase(String dB) {
        Connection conn;
        try {
            // get connection
            conn = CustomConnection.getConnection(dB);
        }
        catch (Exception e) {
            System.out.println("Error connecting to database");
            e.printStackTrace();
            return;
        }

        // create database
        try {
            String sql = "CREATE DATABASE DISCORD_" + dB + "\n" +
                    // support emojis with utf8mb4
                    "DEFAULT CHARSET = utf8mb4 DEFAULT COLLATE = utf8mb4_unicode_ci";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.executeUpdate();
        }
        catch (Exception e) {
            System.out.println("Error creating database");
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
