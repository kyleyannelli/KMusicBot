package MySQL;

import java.sql.Connection;

public class SetupTables {
    public static void setup(String dB) {
        // Create tables

        // Songs
        if(Songs.setupTable(dB)) {
            System.out.println("Songs table created");
        }
        else {
            System.out.println("Error creating Songs table");
        }

        // Users
        if(Users.setupTable(dB)) {
            System.out.println("Users table created");
        }
        else {
            System.out.println("Error creating Users table");
        }
    }
}
