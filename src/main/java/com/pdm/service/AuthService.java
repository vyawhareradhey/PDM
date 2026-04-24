package com.pdm.service;

import com.pdm.core.SessionContext;
import com.pdm.core.User;
import com.pdm.dao.UserDAO;
import java.sql.SQLException;

public class AuthService {
    private UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public boolean login(String username, String password) {
        System.out.println("Attempting login for user: " + username);
        try {
            User user = userDAO.findByUsername(username);
            
            if (user != null) {
                System.out.println("User found. Verifying password...");
                // In production, we'd hash the input password and compare
                // For bootstrap, we're doing a direct string comparison (Assuming simple hash/plaintext stored)
                if (user.getPasswordHash().equals(password)) {
                    System.out.println("Password match! Login success.");
                    SessionContext.getInstance().login(user);
                    return true;
                } else {
                    System.out.println("Password mismatch.");
                }
            } else {
                System.out.println("User not found in database.");
            }
        } catch (SQLException e) {
            System.err.println("Database error during login:");
            e.printStackTrace();
            // Log error properly in real app
        }
        return false;
    }
    
    public void logout() {
        SessionContext.getInstance().logout();
    }

    public boolean register(String username, String password, String fullName, String email, String employeeId, String roleName) {
        try {
            if (userDAO.findByUsername(username) != null) {
                return false; // User already exists
            }
            
            int roleId = userDAO.getRoleIdByName(roleName);
            if (roleId == -1) roleId = 2; // Fallback to Engineer
            
            // Create temporary User object (ID 0 as it's new)
            // Note: We need a Role object.
            // Ideally we should have a RoleDAO or similar, but for speed adding helper in UserDAO for ID lookup
            // And constructing Role object simply.
            com.pdm.core.Role role = new com.pdm.core.Role(roleId, roleName);
            
            com.pdm.core.User newUser = new com.pdm.core.User(0, username, null, role, fullName, email, employeeId);
            
            return userDAO.createUser(newUser, password);
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean resetPassword(String username, String email, String newPassword) {
        try {
            com.pdm.core.User user = userDAO.findByUsernameAndEmail(username, email);
            if (user != null) {
                return userDAO.updatePassword(username, newPassword);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
