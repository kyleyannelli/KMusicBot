package MySQL;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class Songs {
    private String title;
    private String author;
    private long timeSpent;

    public Songs(String title, String author, long timeSpent) {
        this.title = title;
        this.author = author;
        this.timeSpent = timeSpent;
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
                    + "id BIGINT NOT NULL AUTO_INCREMENT,"
                    + "title VARCHAR(100) NOT NULL,"
                    + "author VARCHAR(30) NOT NULL,"
                    + "PRIMARY KEY (id, title, author)"
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
            PreparedStatement statement = conn.prepareStatement("INSERT INTO songs (title, author, time_spent) VALUES (?, ?, ?)");
            // max length of title is 100
            statement.setString(1, title.trim().substring(0, 100));
            // max length of author is 30
            statement.setString(2, author.trim().substring(0, 30));
            // time spent is in milliseconds
            statement.setLong(3, timeSpent);
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
