package com.jbaker454;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {
    private static final String DB_URL = "jdbc:sqlite:database/company.db";

    public static class User {
        public int userId;
        public String username;
        public String role;
        public Integer employeeId;

        public User(int userId, String username, String role, Integer employeeId) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.employeeId = employeeId;
        }
    }

    public static User authenticate(String username, String password) {
        String sql = "SELECT user_id, username, password_hash, role, employee_id FROM users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String hash = rs.getString("password_hash");
                BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hash);
                if (result.verified) {
                    return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getInt("employee_id") // may be null
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void createUser(String username, String password, String role, Integer employeeId) {
        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        String sql = "INSERT INTO users (username, password_hash, role, employee_id) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, hash);
            pstmt.setString(3, role);
            if (employeeId != null) {
                pstmt.setInt(4, employeeId);
            } else {
                pstmt.setNull(4, java.sql.Types.INTEGER);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}