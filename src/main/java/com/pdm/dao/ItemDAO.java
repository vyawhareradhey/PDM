package com.pdm.dao;

import com.pdm.core.Item;
import com.pdm.core.ItemRevision;
import com.pdm.core.User;
import com.pdm.core.Role;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

public class ItemDAO {

    public int createItem(Item item) throws SQLException {
        String sql = "INSERT INTO items (item_id, name, type, description, owner_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, item.getItemId());
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getType());
            stmt.setString(4, item.getDescription());
            stmt.setInt(5, item.getOwner().getId());
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean createRevision(ItemRevision revision) throws SQLException {
        String sql = "INSERT INTO item_revisions (item_pk, revision_id, status, file_name, storage_path, created_by, modified_by, file_mod_timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, revision.getItem().getId());
            stmt.setString(2, revision.getRevisionId());
            stmt.setString(3, revision.getStatus());
            stmt.setString(4, revision.getFileName());
            stmt.setString(5, revision.getStoragePath());
            
            // Handle 0 or default values
            if (revision.getCreatedBy() > 0) stmt.setInt(6, revision.getCreatedBy()); else stmt.setNull(6, java.sql.Types.INTEGER);
            if (revision.getModifiedBy() > 0) stmt.setInt(7, revision.getModifiedBy()); else stmt.setNull(7, java.sql.Types.INTEGER);
            stmt.setTimestamp(8, revision.getFileModTimestamp());
            
            return stmt.executeUpdate() > 0;
        }
    }

    // ... (rest of methods)

    public java.util.List<ItemRevision> getRevisions(int itemPk, Item item) throws SQLException {
        java.util.List<ItemRevision> revs = new java.util.ArrayList<>();
        String sql = "SELECT r.id, r.revision_id, r.status, r.file_name, r.storage_path, " +
                     "r.created_at, r.created_by, uc.username as created_by_name, " +
                     "r.modified_at, r.modified_by, um.username as modified_by_name, " +
                     "r.file_mod_timestamp, r.commit_message " +
                     "FROM item_revisions r " +
                     "LEFT JOIN users uc ON r.created_by = uc.id " +
                     "LEFT JOIN users um ON r.modified_by = um.id " +
                     "WHERE r.item_pk = ? ORDER BY r.revision_id ASC";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setInt(1, itemPk);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    revs.add(new ItemRevision(
                        rs.getInt("id"),
                        item,
                        rs.getString("revision_id"),
                        rs.getString("status"),
                        rs.getString("file_name"),
                        rs.getString("storage_path"),
                        rs.getTimestamp("created_at"),
                        rs.getInt("created_by"),
                        rs.getString("created_by_name"),
                        rs.getTimestamp("modified_at"),
                        rs.getInt("modified_by"),
                        rs.getString("modified_by_name"),
                        rs.getTimestamp("file_mod_timestamp"),
                        rs.getString("commit_message")
                    ));
                }
            }
        }
        return revs;
    }

    public java.util.List<Object[]> getFileVersions(String itemId) throws SQLException {
        java.util.List<Object[]> versions = new java.util.ArrayList<>();
        String sql = "SELECT a.revision_id, a.details, a.timestamp, u.username " +
                     "FROM item_audit_logs a " +
                     "JOIN users u ON a.user_id = u.id " +
                     "WHERE a.item_id = ? AND a.action = 'Checkin_Success' " +
                     "ORDER BY a.id ASC";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setString(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                int versionNumber = 1;
                while (rs.next()) {
                    String revId = rs.getString("revision_id");
                    String details = rs.getString("details");
                    java.sql.Timestamp ts = rs.getTimestamp("timestamp");
                    String user = rs.getString("username");
                    
                    String filePath = "";
                    String msg = details;
                    if (details != null && details.contains(" | ")) {
                        String[] parts = details.split(" \\| ", 2);
                        filePath = parts[0];
                        msg = parts[1];
                        
                        // Extract just the file name from the path
                        int lastSlash = filePath.lastIndexOf('/');
                        if (lastSlash != -1) filePath = filePath.substring(lastSlash + 1);
                    }
                    
                    // Prepend Revision ID to version number for clarity e.g. "Rev 1 - v1"
                    String displayVersion = "Rev " + revId + " - Iteration " + versionNumber++;
                    
                    versions.add(new Object[]{
                        displayVersion,
                        filePath,
                        user,
                        (ts != null ? ts.toString() : ""),
                        msg
                    });
                }
            }
        }
        return versions;
    }

    public String generateNextItemId() throws SQLException {
        // Simple auto-increment for strings 000001, 000002...
        String sql = "SELECT MAX(item_id) FROM items";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                String maxId = rs.getString(1);
                if (maxId != null) {
                    try {
                        int id = Integer.parseInt(maxId);
                        return String.format("%06d", id + 1);
                    } catch (NumberFormatException e) {
                        // Fallback if non-numeric IDs exist
                    }
                }
            }
        }
        return "000001";
    }

    public java.util.List<com.pdm.core.ItemDTO> search(String query) throws SQLException {
        java.util.List<com.pdm.core.ItemDTO> results = new java.util.ArrayList<>();
        
        // This query joins items and item_revisions.
        // For simplicity in Phase 4, we fetch ALL revisions.
        // In a real PDM, you'd typically want only the LATEST revision.
        // We will modify this to basic "search by name or ID" logic.
        
        StringBuilder sql = new StringBuilder(
            "SELECT i.item_id, i.name, i.type, r.revision_id, r.status, r.is_locked, u.username, co_u.username as checked_out_user, ro.role_name as checked_out_role " +
            "FROM items i " +
            "JOIN item_revisions r ON i.id = r.item_pk " +
            "JOIN users u ON i.owner_id = u.id " +
            "LEFT JOIN users co_u ON r.checked_out_by = co_u.id " +
            "LEFT JOIN roles ro ON co_u.role_id = ro.id " +
            "WHERE 1=1 "
        );
        
        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (i.item_id LIKE ? OR i.name LIKE ? OR i.type LIKE ?) ");
        }
        
        sql.append("ORDER BY i.item_id ASC, r.revision_id ASC");
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
             
            if (query != null && !query.trim().isEmpty()) {
                String likeQuery = "%" + query.trim() + "%";
                stmt.setString(1, likeQuery);
                stmt.setString(2, likeQuery);
                stmt.setString(3, likeQuery);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String coUser = rs.getString("checked_out_user");
                    String coRole = rs.getString("checked_out_role");
                    if (coUser != null) {
                        if ("Admin".equalsIgnoreCase(coRole) || "Manager".equalsIgnoreCase(coRole)) {
                            coUser = "<html><font color='red'>Locked</font></html>";
                        }
                    } else {
                        coUser = "";
                    }
                    
                    results.add(new com.pdm.core.ItemDTO(
                        rs.getString("item_id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("revision_id"),
                        rs.getString("status"),
                        rs.getString("username"),
                        coUser
                    ));
                }
            }
        }
        return results;
    }

    public java.util.List<com.pdm.core.ItemDTO> getItemsByFolder(int folderId) throws SQLException {
        java.util.List<com.pdm.core.ItemDTO> results = new java.util.ArrayList<>();
        
        String sql = "SELECT i.item_id, i.name, i.type, r.revision_id, r.status, r.is_locked, u.username, co_u.username as checked_out_user, ro.role_name as checked_out_role " +
                     "FROM items i " +
                     "JOIN item_revisions r ON i.id = r.item_pk " +
                     "JOIN users u ON i.owner_id = u.id " +
                     "LEFT JOIN users co_u ON r.checked_out_by = co_u.id " +
                     "LEFT JOIN roles ro ON co_u.role_id = ro.id " +
                     "JOIN folder_items fi ON i.id = fi.item_id " +
                     "WHERE fi.folder_id = ? " +
                     "ORDER BY i.item_id ASC, r.revision_id ASC";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setInt(1, folderId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String coUser = rs.getString("checked_out_user");
                    String coRole = rs.getString("checked_out_role");
                    if (coUser != null) {
                        if ("Admin".equalsIgnoreCase(coRole) || "Manager".equalsIgnoreCase(coRole)) {
                            coUser = "<html><font color='red'>Locked</font></html>";
                        }
                    } else {
                        coUser = "";
                    }
                    
                    results.add(new com.pdm.core.ItemDTO(
                        rs.getString("item_id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("revision_id"),
                        rs.getString("status"),
                        rs.getString("username"),
                        coUser
                    ));
                }
            }
        }
        return results;
    }

    public Item getItemByItemId(String itemId) throws SQLException {
        String sql = "SELECT i.id, i.item_id, i.name, i.type, i.description, i.owner_id, u.username, u.role_id, r.role_name " +
                     "FROM items i " +
                     "JOIN users u ON i.owner_id = u.id " +
                     "JOIN roles r ON u.role_id = r.id " +
                     "WHERE i.item_id = ?";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Role role = new Role(rs.getInt("role_id"), rs.getString("role_name"));
                    User owner = new User(rs.getInt("owner_id"), rs.getString("username"), "", role, "", "", "");
                    return new Item(
                        rs.getInt("id"),
                        rs.getString("item_id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("description"),
                        owner
                    );
                }
            }
        }
        return null;
    }



    public boolean checkout(int itemPk, String revisionId, int userId, boolean isLocked) throws SQLException {
        String sql = "UPDATE item_revisions SET checked_out_by = ?, is_locked = ? WHERE item_pk = ? AND revision_id = ? AND checked_out_by IS NULL";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setBoolean(2, isLocked);
            stmt.setInt(3, itemPk);
            stmt.setString(4, revisionId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean checkin(int itemPk, String revisionId) throws SQLException {
        String sql = "UPDATE item_revisions SET checked_out_by = NULL, is_locked = FALSE WHERE item_pk = ? AND revision_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, itemPk);
            stmt.setString(2, revisionId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean createNextRevision(int itemPk, String currentRevId, String newStoragePath, int userId, java.sql.Timestamp fileModTimestamp) throws SQLException {
        // Numeric logic: 1 -> 2, 2 -> 3
        int revNum = 1;
        try {
            revNum = Integer.parseInt(currentRevId);
        } catch (NumberFormatException e) {
            // Fallback if legacy A, B exist, just assign a random large number or force "1"
        }
        String nextRev = String.valueOf(revNum + 1);
        
        String sql = "INSERT INTO item_revisions (item_pk, revision_id, status, checked_out_by, is_locked, file_name, storage_path, created_by, modified_by, file_mod_timestamp) " +
                     "SELECT item_pk, ?, 'In Work', NULL, FALSE, file_name, ?, ?, ?, ? FROM item_revisions WHERE item_pk = ? AND revision_id = ?";
                     
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nextRev);
            stmt.setString(2, newStoragePath); // New Path
            stmt.setInt(3, userId); // Created By
            stmt.setInt(4, userId); // Modified By
            stmt.setTimestamp(5, fileModTimestamp);
            stmt.setInt(6, itemPk);
            stmt.setString(7, currentRevId);
            boolean created = stmt.executeUpdate() > 0;
            if (created) {
                updateStatus(itemPk, currentRevId, "Superseded");
            }
            return created;
        }
    }

    public boolean updateStatus(int itemPk, String revisionId, String newStatus) throws SQLException {
        String sql = "UPDATE item_revisions SET status = ? WHERE item_pk = ? AND revision_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setInt(2, itemPk);
            stmt.setString(3, revisionId);
            return stmt.executeUpdate() > 0;
        }
    }
    public boolean updateRevisionFile(int itemPk, String revisionId, String newStoragePath, int userId, java.sql.Timestamp fileModTimestamp, String commitMessage) throws SQLException {
        String sql = "UPDATE item_revisions SET storage_path = ?, modified_by = ?, file_mod_timestamp = ?, commit_message = ? WHERE item_pk = ? AND revision_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStoragePath);
            stmt.setInt(2, userId);
            stmt.setTimestamp(3, fileModTimestamp);
            stmt.setString(4, commitMessage);
            stmt.setInt(5, itemPk);
            stmt.setString(6, revisionId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean renameItem(String itemId, String newName) throws SQLException {
        String sql = "UPDATE items SET name = ? WHERE item_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newName);
            stmt.setString(2, itemId);
            return stmt.executeUpdate() > 0;
        }
    }
    
    public boolean deleteItem(String itemId) throws SQLException {
        // Fetch item Pk first
        Item item = getItemByItemId(itemId);
        if (item == null) return false;
        
        int pk = item.getId();
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Delete references
                try (PreparedStatement s1 = conn.prepareStatement("DELETE FROM folder_items WHERE item_id = ?")) {
                    s1.setInt(1, pk);
                    s1.executeUpdate();
                }
                try (PreparedStatement s2 = conn.prepareStatement("DELETE FROM item_revisions WHERE item_pk = ?")) {
                    s2.setInt(1, pk);
                    s2.executeUpdate();
                }
                try (PreparedStatement s3 = conn.prepareStatement("DELETE FROM items WHERE id = ?")) {
                    s3.setInt(1, pk);
                    s3.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void logAudit(String itemId, String revisionId, int userId, String action, String details) {
        String sql = "INSERT INTO item_audit_logs (item_id, revision_id, user_id, action, details) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            stmt.setString(2, revisionId);
            stmt.setInt(3, userId);
            stmt.setString(4, action);
            stmt.setString(5, details);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean purgeRevisions(int itemPk, String keepRevisionId) throws SQLException {
        String sql = "DELETE FROM item_revisions WHERE item_pk = ? AND revision_id != ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, itemPk);
            stmt.setString(2, keepRevisionId);
            return stmt.executeUpdate() >= 0; 
        }
    }
}
