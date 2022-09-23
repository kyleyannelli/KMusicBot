package MySQL;

import MySQL.Migrations.AddUriColumnToSongsTable;

import java.sql.Connection;

public class SetupTables {
    public static void setup(String dB) {
        // Create tables

        // Songs
        Songs.setupTable(dB);
        // Print success message
        System.out.println("Songs table created successfully");

        // Users
        Users.setupTable(dB);
        // Print success message
        System.out.println("Users table created successfully");

        // Migrations
        runMigrations(dB);
    }

    private static void runMigrations(String db) {
        // Add uri column to songs table
        AddUriColumnToSongsTable.up(db);
    }
}
