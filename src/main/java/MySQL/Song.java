package MySQL;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class Song {
    private String title;
    private String author;
    private long timeSpent;

    public Song(String title, String author, long timeSpent) {
        this.title = title;
        this.author = author;
        this.timeSpent = timeSpent;
    }

    public void save(String dB) {
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
