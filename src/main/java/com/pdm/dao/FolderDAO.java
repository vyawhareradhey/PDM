package com.pdm.dao;

import com.pdm.core.Folder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class FolderDAO {

    public int createFolder(String name, Integer parentId, int ownerId) throws SQLException {
        String sql = "INSERT INTO folders (name, parent_id, owner_id) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, name);
            if (parentId == null || parentId == 0) {
                stmt.setNull(2, Types.INTEGER);
            } else {
                stmt.setInt(2, parentId);
            }
            stmt.setInt(3, ownerId);
            
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public List<Folder> getRootFolders() throws SQLException {
        return getFoldersByParent(null);
    }

    public List<Folder> getSubFolders(int parentId) throws SQLException {
        return getFoldersByParent(parentId);
    }

    public Folder getFolderById(int id) throws SQLException {
        String sql = "SELECT id, name, parent_id, owner_id FROM folders WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int pId = rs.getInt("parent_id");
                    if (rs.wasNull()) pId = 0;
                    return new Folder(rs.getInt("id"), rs.getString("name"), pId, rs.getInt("owner_id"));
                }
            }
        }
        return null;
    }
    
    private List<Folder> getFoldersByParent(Integer parentId) throws SQLException {
        List<Folder> folders = new ArrayList<>();
        String sql;
        if (parentId == null) {
            sql = "SELECT id, name, parent_id, owner_id FROM folders WHERE parent_id IS NULL";
        } else {
            sql = "SELECT id, name, parent_id, owner_id FROM folders WHERE parent_id = ?";
        }
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            if (parentId != null) {
                stmt.setInt(1, parentId);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int pId = rs.getInt("parent_id");
                    if (rs.wasNull()) pId = 0;
                    
                    folders.add(new Folder(
                        rs.getInt("id"),
                        rs.getString("name"),
                        pId,
                        rs.getInt("owner_id")
                    ));
                }
            }
        }
        return folders;
    }

    public boolean addItemToFolder(int folderId, int itemId) throws SQLException {
        String sql = "INSERT INTO folder_items (folder_id, item_id) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, folderId);
            stmt.setInt(2, itemId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        }
    }

    public boolean renameFolder(int folderId, String newName) throws SQLException {
        String sql = "UPDATE folders SET name = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newName);
            stmt.setInt(2, folderId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteFolder(int folderId) throws SQLException {
        // Prevent deleting root / core folders ideally, but basic is fine
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Remove item links first
                try (PreparedStatement s1 = conn.prepareStatement("DELETE FROM folder_items WHERE folder_id = ?")) {
                    s1.setInt(1, folderId);
                    s1.executeUpdate();
                }
                // Delete the folder itself
                try (PreparedStatement s2 = conn.prepareStatement("DELETE FROM folders WHERE id = ?")) {
                    s2.setInt(1, folderId);
                    s2.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex; // Let service handle or cascade issue
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
