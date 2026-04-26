package com.kgm.database;

import com.kgm.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;

public class DatabaseInitializer {

    public static void init() {

        String tableCheck = """
            SELECT name FROM sqlite_master
            WHERE type='table' AND name='employees';
        """;

        String employeeTable = """
            CREATE TABLE IF NOT EXISTS employees (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT UNIQUE,
                phone TEXT,
                department TEXT,
                salary REAL,
                hire_date TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // check if table exists BEFORE creation
            ResultSet rs = stmt.executeQuery(tableCheck);

            if (!rs.next()) {
                stmt.execute(employeeTable);
                System.out.println("=> Schema CREATED (first time): DatabaseInitializer.java");
            } else {
                System.out.println("=> Schema already exists: DatabaseInitializer.java");
            }

        } catch (SQLException e) {
            System.out.println("=> Schema creation failed!: DatabaseInitializer.java");
            e.printStackTrace();
        }
    }
}