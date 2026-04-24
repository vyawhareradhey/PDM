package com.pdm.service;

import com.pdm.core.Item;
import com.pdm.core.ItemRevision;
import com.pdm.core.SessionContext;
import com.pdm.core.User;
import com.pdm.dao.ItemDAO;
import java.sql.SQLException;

public class ItemService {
    private ItemDAO itemDAO;

    public ItemService() {
        this.itemDAO = new ItemDAO();
    }

    public boolean createNewItem(String name, String type, String description, java.io.File file, com.pdm.core.Folder selectedFolder) {
        User currentUser = SessionContext.getInstance().getCurrentUser();
        if (currentUser == null) {
            System.err.println("Error: No user logged in.");
            return false;
        }
        
        if (file == null || !file.exists()) {
             System.err.println("Error: File is mandatory.");
             return false;
        }

        try {
            // 1. Generate ID FIRST so we can use it for file naming
            String itemId = itemDAO.generateNextItemId();
            
            // 0. Process File (Vault Storage) Map locally
            com.pdm.service.FolderService fs = new com.pdm.service.FolderService();
            String storageDir = fs.getPhysicalPath(selectedFolder);
            
            java.io.File vaultDir = new java.io.File(storageDir);
            if (!vaultDir.exists()) vaultDir.mkdirs();
            
            String originalName = file.getName();
            String cleanOriginalName = originalName.replaceAll("[^a-zA-Z0-9.-]", "_");
            
            String storageName = itemId + "_" + cleanOriginalName;
            java.io.File destFile = new java.io.File(vaultDir, storageName);
            
            // Simple copy
            java.nio.file.Files.copy(file.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            String storagePath = destFile.getAbsolutePath();
            
            // 2. Create Item Master
            // We create a temporary item object to pass data, ID is 0 initially
            Item newItem = new Item(0, itemId, name, type, description, currentUser);
            
            int itemPk = itemDAO.createItem(newItem);
            
            if (itemPk != -1) {
                // Re-create item with correct PK
                Item persistedItem = new Item(itemPk, itemId, name, type, description, currentUser);
                
                // 3. Create Revision "1"
                ItemRevision revision = new ItemRevision(0, persistedItem, "1", "In Work", originalName, storagePath);
                revision.setCreatedBy(currentUser.getId());
                revision.setModifiedBy(currentUser.getId());
                
                boolean revCreated = itemDAO.createRevision(revision);
                if (revCreated && selectedFolder != null) {
                    try {
                        new com.pdm.dao.FolderDAO().addItemToFolder(selectedFolder.getId(), itemPk);
                    } catch(SQLException ex) {
                        ex.printStackTrace();
                    }
                }
                return revCreated;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public java.util.List<com.pdm.core.ItemDTO> searchItems(String query) {
        try {
            return itemDAO.search(query);
        } catch (SQLException e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    public java.util.List<com.pdm.core.ItemDTO> getItemsByFolder(int folderId) {
        try {
            return itemDAO.getItemsByFolder(folderId);
        } catch (SQLException e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    public boolean checkoutItem(String itemId, String revisionId) {
        User currentUser = SessionContext.getInstance().getCurrentUser();
        if (currentUser == null) return false;
        
        try {
            com.pdm.core.Item item = itemDAO.getItemByItemId(itemId);
            if (item != null) {
                if (itemDAO.checkout(item.getId(), revisionId, currentUser.getId(), false)) {
                    // Success! Now copy to Workspace and open.
                    try {
                        java.util.List<com.pdm.core.ItemRevision> revs = itemDAO.getRevisions(item.getId(), item);
                        for (com.pdm.core.ItemRevision r : revs) {
                            if (r.getRevisionId().equals(revisionId)) {
                                String vaultPath = r.getStoragePath();
                                if (vaultPath != null && !vaultPath.trim().isEmpty()) {
                                    java.io.File vaultFile = new java.io.File(vaultPath);
                                    if (vaultFile.exists()) {
                                        // 1. Create Workspace Dir
                                        String workspaceDir = System.getProperty("user.home") + "/.pdm/workspace";
                                        new java.io.File(workspaceDir).mkdirs();
                                        
                                        // 2. Generate Workspace Filename: {itemId}_{rev}_{filename}
                                        String wsName = itemId + "_" + revisionId + "_" + r.getFileName();
                                        java.io.File wsFile = new java.io.File(workspaceDir, wsName);
                                        
                                        // 3. Copy Vault -> Workspace
                                        java.nio.file.Files.copy(vaultFile.toPath(), wsFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                        
                                        // 4. Open Workspace File
                                        if (java.awt.Desktop.isDesktopSupported()) {
                                            java.awt.Desktop.getDesktop().open(wsFile);
                                        }
                                        itemDAO.logAudit(itemId, revisionId, currentUser.getId(), "Checkout", "File checked out to workspace");
                                    }
                                }
                                break;
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("Failed to auto-open file: " + ex.getMessage());
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean checkinItem(String itemId, String revisionId, String commitMessage) {
        try {
            com.pdm.core.Item item = itemDAO.getItemByItemId(itemId);
            User currentUser = SessionContext.getInstance().getCurrentUser();
            
            if (item != null && currentUser != null) {
                // Find current revision to get file info
                java.util.List<com.pdm.core.ItemRevision> revs = itemDAO.getRevisions(item.getId(), item);
                com.pdm.core.ItemRevision currentRev = null;
                for (com.pdm.core.ItemRevision r : revs) {
                    if (r.getRevisionId().equals(revisionId)) {
                        currentRev = r;
                        break;
                    }
                }
                
                if (currentRev != null) {
                    // Logic:
                    // 1. Locate Workspace File
                    String workspaceDir = System.getProperty("user.home") + "/.pdm/workspace";
                    String wsName = itemId + "_" + revisionId + "_" + currentRev.getFileName();
                    java.io.File wsFile = new java.io.File(workspaceDir, wsName);
                    
                    // 2. Locate Vault File
                    java.io.File vaultFile = new java.io.File(currentRev.getStoragePath());
                    
                    if (wsFile.exists() && vaultFile.exists()) {
                        // 3. Compare content (size/lastModified simple check, real app uses hash)
                        boolean modified = wsFile.length() != vaultFile.length(); 
                        // Note: Timestamp check is tricky if copy preserves it. Let's rely on simple length or assume modified if verified.
                        // Better: SHA Check.
                        try {
                             byte[] wsBytes = java.nio.file.Files.readAllBytes(wsFile.toPath());
                             byte[] vaultBytes = java.nio.file.Files.readAllBytes(vaultFile.toPath());
                             if (java.util.Arrays.equals(wsBytes, vaultBytes)) {
                                 itemDAO.logAudit(itemId, revisionId, currentUser.getId(), "Checkin_Failed", "No changes detected");
                                 javax.swing.JOptionPane.showMessageDialog(null, "No changes detected. Save the file before checking in.");
                                 return false;
                             }
                        } catch (java.io.IOException e) {
                             e.printStackTrace();
                             return false;
                        }

                        // 4. Versioning: Copy Workspace -> Vault (New Name)
                        // Retrieve the old file parent to stack the new version natively inside the same structural hierarchy location!
                        java.io.File locStorage = vaultFile.getParentFile();
                        if (!locStorage.exists()) locStorage.mkdirs();
                        
                        String originalName = currentRev.getFileName();
                        String cleanOriginalName = originalName.replaceAll("[^a-zA-Z0-9.-]", "_");
                        
                        String newStorageName = itemId + "_" + cleanOriginalName;
                        java.io.File newVaultFile = new java.io.File(locStorage, newStorageName);
                        
                        try {
                            java.nio.file.Files.copy(wsFile.toPath(), newVaultFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            
                            // 5. UPDATE Current Revision (Same-Rev Check-In)
                            java.sql.Timestamp fileMod = new java.sql.Timestamp(wsFile.lastModified());
                            
                            if (itemDAO.updateRevisionFile(item.getId(), revisionId, newVaultFile.getAbsolutePath(), currentUser.getId(), fileMod, commitMessage)) {
                                itemDAO.logAudit(itemId, revisionId, currentUser.getId(), "Checkin_Success", commitMessage);
                                return itemDAO.checkin(item.getId(), revisionId); // Just unlock
                            }
                            
                        } catch (java.io.IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // Fallback logic if no file attached: just unlock
                         return itemDAO.checkin(item.getId(), revisionId);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean promoteItem(String itemId, String revisionId) {
        User currentUser = SessionContext.getInstance().getCurrentUser();
        if (currentUser == null) return false;
        
        // Validation: Only Admin can promote
        if (!"Admin".equalsIgnoreCase(currentUser.getRole().getName())) {
            javax.swing.JOptionPane.showMessageDialog(null, "Only Administrators can Promote/Release items.");
            return false;
        }

        try {
            com.pdm.core.Item item = itemDAO.getItemByItemId(itemId);
            if (item != null) {
                // Fetch detail to check status & lock first (Optimization: could add specific DAO method)
                // For now, let's just use getRevisions and filter in memory or lazy way
                java.util.List<com.pdm.core.ItemRevision> revs = itemDAO.getRevisions(item.getId(), item);
                for (com.pdm.core.ItemRevision r : revs) {
                    if (r.getRevisionId().equals(revisionId)) {
                        
                        // Validation 1: Cannot promote if checked out
                        
                        String newStatus = null;
                        if ("In Work".equalsIgnoreCase(r.getStatus())) {
                            newStatus = "Released";
                        } else if ("Released".equalsIgnoreCase(r.getStatus())) {
                            newStatus = "Obsolete";
                        }
                        
                        if (newStatus != null) {
                            return itemDAO.updateStatus(item.getId(), revisionId, newStatus);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    public boolean isItemModified(String itemId, String revisionId) {
        try {
            com.pdm.core.Item item = itemDAO.getItemByItemId(itemId);
            if (item != null) {
                java.util.List<com.pdm.core.ItemRevision> revs = itemDAO.getRevisions(item.getId(), item);
                for (com.pdm.core.ItemRevision r : revs) {
                    if (r.getRevisionId().equals(revisionId)) {
                        String workspaceDir = System.getProperty("user.home") + "/.pdm/workspace";
                        String wsName = itemId + "_" + revisionId + "_" + r.getFileName();
                        java.io.File wsFile = new java.io.File(workspaceDir, wsName);
                        java.io.File vaultFile = new java.io.File(r.getStoragePath());
                        
                        if (wsFile.exists() && vaultFile.exists()) {
                            try {
                                byte[] wsBytes = java.nio.file.Files.readAllBytes(wsFile.toPath());
                                byte[] vaultBytes = java.nio.file.Files.readAllBytes(vaultFile.toPath());
                                return !java.util.Arrays.equals(wsBytes, vaultBytes);
                            } catch (java.io.IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean unlockItem(String itemId, String revisionId) {
        try {
            com.pdm.core.Item item = itemDAO.getItemByItemId(itemId);
            if (item != null) {
                return itemDAO.checkin(item.getId(), revisionId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean lockItem(String itemId, String revisionId) {
        User currentUser = SessionContext.getInstance().getCurrentUser();
        if (currentUser == null) return false;
        
        try {
            com.pdm.core.Item item = itemDAO.getItemByItemId(itemId);
            if (item != null) {
                if (itemDAO.checkout(item.getId(), revisionId, currentUser.getId(), true)) {
                    itemDAO.logAudit(itemId, revisionId, currentUser.getId(), "Lock", "Item Locked explicitly");
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean reviseItem(String itemId, String revisionId) {
        User currentUser = SessionContext.getInstance().getCurrentUser();
        if (currentUser == null) return false;
        
        try {
            com.pdm.core.Item item = itemDAO.getItemByItemId(itemId);
            if (item != null) {
                java.util.List<com.pdm.core.ItemRevision> revs = itemDAO.getRevisions(item.getId(), item);
                for (com.pdm.core.ItemRevision r : revs) {
                    if (r.getRevisionId().equals(revisionId)) {
                        // Create a new revision copying the storage path of the previous
                        java.sql.Timestamp currentModTime = new java.sql.Timestamp(System.currentTimeMillis());
                        return itemDAO.createNextRevision(item.getId(), revisionId, r.getStoragePath(), currentUser.getId(), currentModTime);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean renameItem(String itemId, String newName) {
        if (newName == null || newName.trim().isEmpty()) return false;
        try {
            return itemDAO.renameItem(itemId, newName.trim());
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteItem(String itemId) {
        try {
            return itemDAO.deleteItem(itemId);
        } catch (Exception e) { 
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean purgeItemRevisions(String itemId, String keepRevisionId) {
        User currentUser = SessionContext.getInstance().getCurrentUser();
        if (currentUser == null) return false;
        if (!"Admin".equalsIgnoreCase(currentUser.getRole().getName()) && !"Manager".equalsIgnoreCase(currentUser.getRole().getName())) {
            javax.swing.JOptionPane.showMessageDialog(null, "Only Admin or Manager can Purge iterations.");
            return false;
        }

        try {
            com.pdm.core.Item item = itemDAO.getItemByItemId(itemId);
            if (item != null) {
                if (itemDAO.purgeRevisions(item.getId(), keepRevisionId)) {
                    itemDAO.logAudit(itemId, keepRevisionId, currentUser.getId(), "Purge", "Purged prior revisions");
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
