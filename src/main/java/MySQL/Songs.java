package MySQL;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
                    + "id BIGINT NOT NULL AUTO_INCREMENT,"
                    + "title VARCHAR(100) NOT NULL,"
                    + "author VARCHAR(30) NOT NULL,"
                    + "PRIMARY KEY (id),"
                    + "UNIQUE (title, author)"
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

        PreparedStatement statement;
        try {
            // create prepared statement
            statement = conn.prepareStatement("INSERT INTO songs (title, author) VALUES (?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
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
            try {
                // if song already exists, get id
                statement = conn.prepareStatement("SELECT id FROM songs WHERE title = ? AND author = ?");
                statement.setString(1, title);
                statement.setString(2, author);
                ResultSet rs = statement.executeQuery();
                rs.next();
                return rs.getLong("id");
            }
            catch (Exception e2) {
                System.out.println("Error saving song");
                e.printStackTrace();
                e2.printStackTrace();
                return -1;
            }
        }

        try {
            // get id of song
            long id = statement.getGeneratedKeys().getLong(1);
            // close connection
            conn.close();
            // return id
            return id;
        }
        catch (Exception e) {
            System.out.println("Error closing connection");
            e.printStackTrace();
            return -1;
        }
    }

    public static long getSongIdByTrack(AudioTrack track, long dB) {
        try {
            // get song id
            String title = track.getInfo().title;
            String author = track.getInfo().author;

            // get connection
            Connection conn = CustomConnection.getConnection("DISCORD_" + dB);

            // create prepared statement
            PreparedStatement statement = conn.prepareStatement("SELECT id FROM songs WHERE title = ? AND author = ?", PreparedStatement.RETURN_GENERATED_KEYS);
            statement.setString(1, title);
            statement.setString(2, author);
            // execute statement
            statement.execute();
            ResultSet rs = statement.getResultSet();
            while(rs.next()) {
                // get id
                long id = rs.getLong("id");
                // close connection
                conn.close();
                // return id
                return id;
            }
            // get id
            return -1;
        }
        catch (Exception e) {
            System.out.println("Error getting song id");
            e.printStackTrace();
            return -1;
        }
    }
}
