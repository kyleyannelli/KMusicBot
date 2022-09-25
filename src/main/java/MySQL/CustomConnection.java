package MySQL;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;

public abstract class CustomConnection {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String URL = dotenv.get("DB_URL");
    private static final String USER = dotenv.get("DB_USER");
    private static final String PASSWORD = dotenv.get("DB_USER_PASSWORD");

    public static Connection getConnection(String dB) {
        try {
            return DriverManager.getConnection(URL + dB, USER, PASSWORD);
        }
        catch (Exception e) {
            System.out.println("Error connecting to " + URL);
            e.printStackTrace();
            return null;
        }
    }
}
