package com.pdm.util;

import com.pdm.dao.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class DatabaseSeeder {
    public static void main(String[] args) {
        System.out.println("Starting Database Seeder...");
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            System.out.println("Connected to database successfully.");
            
            // 0. Drop existing tables to verify clean state (Bootstrap only!)
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS folder_items CASCADE");
                stmt.execute("DROP TABLE IF EXISTS folders CASCADE");
                stmt.execute("DROP TABLE IF EXISTS product_structure CASCADE");
                stmt.execute("DROP TABLE IF EXISTS item_audit_logs CASCADE");
                stmt.execute("DROP TABLE IF EXISTS item_revisions CASCADE");
                stmt.execute("DROP TABLE IF EXISTS items CASCADE");
                stmt.execute("DROP TABLE IF EXISTS users CASCADE");
                stmt.execute("DROP TABLE IF EXISTS roles CASCADE");
                System.out.println("Dropped existing tables.");
            }

            // 1. Create Roles Table
            String createRoles = "CREATE TABLE IF NOT EXISTS roles (" +
                    "id SERIAL PRIMARY KEY, " +
                    "role_name VARCHAR(50) NOT NULL UNIQUE)";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createRoles);
                System.out.println("Verified 'roles' table.");
            }
            
            // 2. Create Users Table
            String createUsers = "CREATE TABLE IF NOT EXISTS users (" +
                    "id SERIAL PRIMARY KEY, " +
                    "username VARCHAR(50) NOT NULL UNIQUE, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "role_id INT NOT NULL, " +
                    "full_name VARCHAR(100), " +
                    "email VARCHAR(100) NOT NULL, " +
                    "employee_id VARCHAR(50), " +
                    "FOREIGN KEY (role_id) REFERENCES roles(id))";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createUsers);
                System.out.println("Verified 'users' table.");
            }
            
            // 3. Create Items Table
            String createItems = "CREATE TABLE IF NOT EXISTS items (" +
                    "id SERIAL PRIMARY KEY, " +
                    "item_id VARCHAR(50) NOT NULL UNIQUE, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "type VARCHAR(50) NOT NULL, " +
                    "description TEXT, " +
                    "owner_id INT NOT NULL, " +
                    "FOREIGN KEY (owner_id) REFERENCES users(id))";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createItems);
                System.out.println("Verified 'items' table.");
            }

            // 4. Create Item Revisions Table
            String createRevisions = "CREATE TABLE IF NOT EXISTS item_revisions (" +
                    "id SERIAL PRIMARY KEY, " +
                    "item_pk INT NOT NULL, " +
                    "revision_id VARCHAR(10) NOT NULL, " +
                    "status VARCHAR(50) NOT NULL, " +
                    "checked_out_by INT DEFAULT NULL, " +
                    "is_locked BOOLEAN DEFAULT FALSE, " +
                    "file_name VARCHAR(255), " +
                    "storage_path VARCHAR(255), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "created_by INT, " +
                    "modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "modified_by INT, " +
                    "file_mod_timestamp TIMESTAMP NULL, " +
                    "commit_message TEXT, " +
                    "FOREIGN KEY (item_pk) REFERENCES items(id), " +
                    "FOREIGN KEY (checked_out_by) REFERENCES users(id), " +
                    "FOREIGN KEY (created_by) REFERENCES users(id), " +
                    "FOREIGN KEY (modified_by) REFERENCES users(id), " +
                    "UNIQUE (item_pk, revision_id))";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createRevisions);
                System.out.println("Verified 'item_revisions' table.");
            }
            
            // 5. Create Product Structure Table
            String createStructure = "CREATE TABLE IF NOT EXISTS product_structure (" +
                    "id SERIAL PRIMARY KEY, " +
                    "parent_rev_id INT NOT NULL, " +
                    "child_item_id INT NOT NULL, " +
                    "quantity INT DEFAULT 1, " +
                    "FOREIGN KEY (parent_rev_id) REFERENCES item_revisions(id), " +
                    "FOREIGN KEY (child_item_id) REFERENCES items(id))";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createStructure);
                System.out.println("Verified 'product_structure' table.");
            }
            
            // 6. Create Folders Table
            String createFolders = "CREATE TABLE IF NOT EXISTS folders (" +
                    "id SERIAL PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "parent_id INT DEFAULT NULL, " +
                    "owner_id INT NOT NULL, " +
                    "FOREIGN KEY (parent_id) REFERENCES folders(id), " +
                    "FOREIGN KEY (owner_id) REFERENCES users(id))";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createFolders);
                System.out.println("Verified 'folders' table.");
            }
            
            // 7. Create Folder Items Table
            String createFolderItems = "CREATE TABLE IF NOT EXISTS folder_items (" +
                    "folder_id INT NOT NULL, " +
                    "item_id INT NOT NULL, " +
                    "PRIMARY KEY (folder_id, item_id), " +
                    "FOREIGN KEY (folder_id) REFERENCES folders(id), " +
                    "FOREIGN KEY (item_id) REFERENCES items(id))";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createFolderItems);
                System.out.println("Verified 'folder_items' table.");
            }

            // 8. Seed Roles
            String[] roles = {"Admin", "Engineer", "Manager"};
            String insertRole = "INSERT INTO roles (role_name) VALUES (?) ON CONFLICT DO NOTHING";
            try (PreparedStatement result = conn.prepareStatement(insertRole)) {
                for (String role : roles) {
                    result.setString(1, role);
                    result.executeUpdate();
                }
                System.out.println("Seeded roles.");
            }

            // 4. Seed Admin User
            int adminRoleId = -1;
            try (Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("SELECT id FROM roles WHERE role_name = 'Admin'")) {
                if (rs.next()) adminRoleId = rs.getInt("id");
            }

            if (adminRoleId != -1) {
                String checkAdmin = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
                boolean adminExists = false;
                try (Statement stmt = conn.createStatement();
                     java.sql.ResultSet rs = stmt.executeQuery(checkAdmin)) {
                    if (rs.next() && rs.getInt(1) > 0) adminExists = true;
                }

                if (!adminExists) {
                    String insertAdmin = "INSERT INTO users (username, password_hash, role_id, full_name, email, employee_id) " +
                            "VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(insertAdmin)) {
                        stmt.setString(1, "admin");
                        stmt.setString(2, "admin123");
                        stmt.setInt(3, adminRoleId);
                        stmt.setString(4, "System Administrator");
                        stmt.setString(5, "admin@example.com");
                        stmt.setString(6, "EMP-001");
                        stmt.executeUpdate();
                        System.out.println("Seeded 'admin' user (password: admin123).");
                    }
                } else {
                    System.out.println("'admin' user already exists.");
                }
            }
            
            System.out.println("Database seeding complete. No sample items created (Clean State).");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Seeding failed! Check db.properties and ensure the database exists.");
        }
    }
}
