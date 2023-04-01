package MySQL.Migrations;

import MySQL.CustomConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AddUriColumnToSongsTable {
    public static void up(String dB) {
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
        // add column
        try {
            // check if column exists by SHOW COLUMNS
            String sql = "SHOW COLUMNS FROM songs LIKE 'uri'";
            assert conn != null;
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()) {
                // column exists
                System.out.println("Column uri already exists on songs table on database DISCORD_" + dB);
                return;
            }
            // column does not exist
            sql = "ALTER TABLE songs ADD uri VARCHAR(255)";
            stmt = conn.prepareStatement(sql);
            stmt.executeUpdate();
        }
        catch (Exception e) {
            System.out.println("Error adding column");
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
        System.out.println("Column uri added successfully to songs table on database DISCORD_" + dB);
    }

    public static void down(String dB) {
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
        // drop column
        try {
            // check if column exists by SHOW COLUMNS
            String sql = "SHOW COLUMNS FROM songs LIKE 'uri'";
            assert conn != null;
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            if(!rs.next()) {
                // column does not exist
                System.out.println("Column uri does not exist on songs table on database DISCORD_" + dB);
                return;
            }
            // column exists
            sql = "ALTER TABLE songs DROP COLUMN uri";
            stmt = conn.prepareStatement(sql);
            stmt.executeUpdate();
        }
        catch (Exception e) {
            System.out.println("Error dropping column");
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
        System.out.println("Column uri dropped successfully from songs table on database DISCORD_" + dB);
    }
}
