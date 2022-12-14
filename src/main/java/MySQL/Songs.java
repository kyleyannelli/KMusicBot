package MySQL;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Songs {
    private String title;
    private String author;
    private String uri;

    public Songs(String title, String author) {
        this.title = title;
        this.author = author;
        this.uri = "";
    }

    public Songs(String title, String author, String uri) {
        this.title = title;
        this.author = author;
        this.uri = uri;
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
            statement = conn.prepareStatement("INSERT INTO songs (title, author, uri) VALUES (?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
            // max length of title is 100. if title is longer than 100, truncate it
            if(title.trim().length() > 100) {
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
            // max length of uri is 255. if uri is longer than 255, truncate it
            if (uri.trim().length() > 255) {
                statement.setString(3, uri.trim().substring(0, 255));
            }
            else {
                statement.setString(3, uri.trim());
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
                long id = rs.getLong("id");
                try {
                    conn.close();
                }
                catch (Exception e2) {
                    System.out.println("Error closing connection");
                    e.printStackTrace();
                    e2.printStackTrace();
                    return -1;
                }
                return id;
            }
            catch (Exception e2) {
                System.out.println("Error getting id");
                e2.printStackTrace();
                return -1;
            }
        }

        try {
            // if song already exists, get id
            statement = conn.prepareStatement("SELECT id FROM songs WHERE title = ? AND author = ?");
            statement.setString(1, title);
            statement.setString(2, author);
            ResultSet rs = statement.executeQuery();
            rs.next();
            long id = rs.getLong("id");
            // update uri
            statement = conn.prepareStatement("UPDATE songs SET uri = ? WHERE id = ?");
            statement.setString(1, uri);
            statement.setLong(2, id);
            statement.execute();
            try {
                conn.close();
            }
            catch (Exception e) {
                System.out.println("Error closing connection");
                e.printStackTrace();
                return -1;
            }
            return id;
        }
        catch (Exception e2) {
            System.out.println("Error getting id");
            e2.printStackTrace();
            return -1;
        }
    }

    public static long getSongIdByTrack(AudioTrack track, long dB) {
        try {
            // get song id
            String title = track.getInfo().title.trim();
            String author = track.getInfo().author.trim();

            // get connection
            Connection conn = CustomConnection.getConnection("DISCORD_" + dB);

            // create prepared statement
            PreparedStatement statement = conn.prepareStatement("SELECT id FROM songs WHERE title = ? AND author = ?", PreparedStatement.RETURN_GENERATED_KEYS);
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
            ResultSet rs = statement.getResultSet();
            rs.next();
            // get id
            long id = rs.getLong("id");
            // close connection
            conn.close();
            // return id
            return id;
        }
        catch (Exception e) {
            System.out.println("Error getting song id");
            e.printStackTrace();
            return -1;
        }
    }
}
