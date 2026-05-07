package com.jbaker454;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import at.favre.lib.crypto.bcrypt.BCrypt;

public class DatabaseSetup {
    public static void main(String[] args) {
        String dbUrl = "jdbc:sqlite:database/company.db";
        String schemaFile = "database/DatabaseSetUp.sqlite3-query";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             BufferedReader reader = new BufferedReader(new FileReader(schemaFile))) {

            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sql.append(line).append("\n");
            }

            // Split by semicolon to execute multiple statements
            String[] statements = sql.toString().split(";");
            for (String statement : statements) {
                if (!statement.trim().isEmpty()) {
                    stmt.execute(statement.trim());
                }
            }

            System.out.println("Database created successfully!");

            // Now add users table
            String createUsersTable = """
                CREATE TABLE users (
                  user_id INTEGER PRIMARY KEY,
                  username TEXT UNIQUE NOT NULL,
                  password_hash TEXT NOT NULL,
                  role TEXT NOT NULL CHECK (role IN ('EMPLOYEE', 'HR', 'ADMIN')),
                  employee_id INTEGER,
                  FOREIGN KEY (employee_id) REFERENCES employee(employee_id)
                );
                """;
            stmt.execute(createUsersTable);

            // Insert default HR user with password "password"
            String password = "password";
            String hash = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults().hashToString(12, password.toCharArray());
            String insertHR = "INSERT INTO users (username, password_hash, role) VALUES ('HR0001', '" + hash + "', 'HR');";
            stmt.execute(insertHR);

            System.out.println("Users table created and HR user added!");

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }
}