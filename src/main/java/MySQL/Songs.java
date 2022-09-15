package MySQL;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class Songs {
    private String title;
    private String author;

    public Songs(String title, String author) {
        this.title = title;
        this.author = author;
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
            String sql = "CREATE TABLE IF NOT EXISTS songs ("
                    + "title VARCHAR(100) NOT NULL,"
                    + "author VARCHAR(30) NOT NULL,"
                    + "PRIMARY KEY (title, author)"
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

        try {
            // create prepared statement
            PreparedStatement statement = conn.prepareStatement("INSERT INTO songs (title, author) VALUES (?, ?)");
            // max length of title is 100. if title is longer than 100, truncate it
            if (title.trim().length() > 100) {
                statement.setString(1, title.trim().substring(0, 100));
            }
            else {
                statement.setString(1, title.trim());
            }
            // max length of author is 30. if author is longer than 30, truncate it
            if (author.trim().length() > 30) {
                statement.setString(2, author.trim().substring(0, 30));
            }
            else {
                statement.setString(2, author.trim());
            }
            // execute statement
            statement.execute();
        }
        catch (Exception e) {
            System.out.println("Error saving song");
            e.printStackTrace();
            return;
        }

        try {
            // close connection
            conn.close();
        }
        catch (Exception e) {
            System.out.println("Error closing connection");
            e.printStackTrace();
            return;
        }
    }
}
