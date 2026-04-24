package com.pdm.dao;

import com.pdm.core.Role;
import com.pdm.core.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT u.id, u.username, u.password_hash, u.role_id, u.full_name, u.email, u.employee_id, r.role_name " +
                     "FROM users u " +
                     "JOIN roles r ON u.role_id = r.id " +
                     "WHERE u.username = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            System.out.println("Executing query: " + sql + " [param=" + username + "]");
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Role role = new Role(rs.getInt("role_id"), rs.getString("role_name"));
                    return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        role,
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("employee_id")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDAO Error: " + e.getMessage());
            throw e;
        }
        return null;
    }

    public User findByUsernameAndEmail(String username, String email) throws SQLException {
        String sql = "SELECT u.id, u.username, u.password_hash, u.role_id, u.full_name, u.email, u.employee_id, r.role_name " +
                     "FROM users u " +
                     "JOIN roles r ON u.role_id = r.id " +
                     "WHERE u.username = ? AND u.email = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.setString(2, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Role role = new Role(rs.getInt("role_id"), rs.getString("role_name"));
                    return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        role,
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("employee_id")
                    );
                }
            }
        }
        return null;
    }

    public boolean createUser(User user, String password) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, role_id, full_name, email, employee_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, password); // In real app: hash this
            stmt.setInt(3, user.getRole().getId());
            stmt.setString(4, user.getFullName());
            stmt.setString(5, user.getEmail());
            stmt.setString(6, user.getEmployeeId());
            
            int rows = stmt.executeUpdate();
            return rows > 0;
        }
    }

    public boolean updatePassword(String username, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE username = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, newPassword); // In real app: hash this
            stmt.setString(2, username);
            
            int rows = stmt.executeUpdate();
            return rows > 0;
        }
    }

    public int getRoleIdByName(String roleName) throws SQLException {
        String sql = "SELECT id FROM roles WHERE role_name = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roleName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return -1;
    }
}
