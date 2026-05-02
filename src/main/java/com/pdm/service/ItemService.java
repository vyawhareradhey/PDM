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
            
            String originalName = file.getName();
            String cleanOriginalName = originalName.replaceAll("[^a-zA-Z0-9.-]", "_");
            String storageName = "1_" + cleanOriginalName;
            
            // New Alternative 2 Structure: items/[folder]/[itemId]/[rev]_[fileName]
            String cloudPath = storageDir + "/" + itemId + "/" + storageName;
            
            // Upload to Supabase Cloud
            SupabaseStorageClient cloudClient = new SupabaseStorageClient();
            boolean uploaded = cloudClient.uploadFile("pdm-vault", cloudPath, file);
            if (!uploaded) {
                System.err.println("Cloud Upload Failed!");
                return false;
            }
            
            String storagePath = cloudPath;
            
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
                                        // 1. Create Workspace Dir
                                        String workspaceDir = System.getProperty("user.home") + "/.pdm/workspace";
                                        new java.io.File(workspaceDir).mkdirs();
                                        
                                        // 2. Generate Workspace Filename: {itemId}_{rev}_{filename}
                                        String wsName = itemId + "_" + revisionId + "_" + r.getFileName();
                                        java.io.File wsFile = new java.io.File(workspaceDir, wsName);
                                        
                                        // 3. Download from Supabase Cloud -> Workspace
                                        SupabaseStorageClient cloudClient = new SupabaseStorageClient();
                                        boolean downloaded = cloudClient.downloadFile("pdm-vault", vaultPath, wsFile);
                                        
                                        if (downloaded && wsFile.exists()) {
                                            // 4. Open Workspace File
                                            if (java.awt.Desktop.isDesktopSupported()) {
                                                java.awt.Desktop.getDesktop().open(wsFile);
                                            }
                                            itemDAO.logAudit(itemId, revisionId, currentUser.getId(), "Checkout", "File downloaded from cloud to workspace");
                                        } else {
                                            System.err.println("Failed to download file from Supabase Cloud.");
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
                    
                    // 2. Cloud Path
                    String cloudPath = currentRev.getStoragePath();
                    
                    if (wsFile.exists()) {
                        // 3. Compare content using local workspace file (Skip length compare since cloud file isn't local)
                        
                        // 4. Versioning: Upload Workspace -> Supabase Vault (Overwrite)
                        SupabaseStorageClient cloudClient = new SupabaseStorageClient();
                        boolean uploaded = cloudClient.uploadFile("pdm-vault", cloudPath, wsFile);
                        
                        if (uploaded) {
                            // 5. UPDATE Current Revision (Same-Rev Check-In)
                            java.sql.Timestamp fileMod = new java.sql.Timestamp(wsFile.lastModified());
                            
                            if (itemDAO.updateRevisionFile(item.getId(), revisionId, cloudPath, currentUser.getId(), fileMod, commitMessage)) {
                                itemDAO.logAudit(itemId, revisionId, currentUser.getId(), "Checkin_Success", "Cloud Vault Update: " + commitMessage);
                                return itemDAO.checkin(item.getId(), revisionId); // Just unlock
                            }
                        } else {
                            System.err.println("Check-in Failed: Cloud Upload Error");
                            return false;
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
                        
                        // We simply assume modified if it exists locally in the workspace.
                        // Full byte comparison against cloud requires downloading it again, which is heavy.
                        return wsFile.exists();
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
                        
                        // Figure out next revision number
                        int revNum = 1;
                        try {
                            revNum = Integer.parseInt(revisionId);
                        } catch (Exception e) {}
                        String nextRev = String.valueOf(revNum + 1);
                        
                        String oldPath = r.getStoragePath();
                        String newPath = oldPath;
                        
                        // Generate the new physical cloud path and duplicate the file
                        if (oldPath != null && oldPath.contains("/")) {
                            int lastSlash = oldPath.lastIndexOf('/');
                            String directory = oldPath.substring(0, lastSlash);
                            String fileName = r.getFileName();
                            String cleanOriginalName = fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
                            newPath = directory + "/" + nextRev + "_" + cleanOriginalName;
                            
                            SupabaseStorageClient cloudClient = new SupabaseStorageClient();
                            try {
                                java.io.File tempFile = java.io.File.createTempFile("pdm_revise", ".tmp");
                                if (cloudClient.downloadFile("pdm-vault", oldPath, tempFile)) {
                                    cloudClient.uploadFile("pdm-vault", newPath, tempFile);
                                }
                                tempFile.delete();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }

                        java.sql.Timestamp currentModTime = new java.sql.Timestamp(System.currentTimeMillis());
                        return itemDAO.createNextRevision(item.getId(), revisionId, newPath, currentUser.getId(), currentModTime);
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
            com.pdm.core.Item item = itemDAO.getItemByItemId(itemId);
            if (item != null) {
                java.util.List<com.pdm.core.ItemRevision> revs = itemDAO.getRevisions(item.getId(), item);
                SupabaseStorageClient cloudClient = new SupabaseStorageClient();
                for (com.pdm.core.ItemRevision r : revs) {
                    if (r.getStoragePath() != null && !r.getStoragePath().isEmpty()) {
                        cloudClient.deleteFile("pdm-vault", r.getStoragePath());
                    }
                }
            }
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
                // Delete physical files from cloud for purged revisions
                java.util.List<com.pdm.core.ItemRevision> revs = itemDAO.getRevisions(item.getId(), item);
                SupabaseStorageClient cloudClient = new SupabaseStorageClient();
                for (com.pdm.core.ItemRevision r : revs) {
                    if (!r.getRevisionId().equals(keepRevisionId)) {
                        if (r.getStoragePath() != null && !r.getStoragePath().trim().isEmpty()) {
                            cloudClient.deleteFile("pdm-vault", r.getStoragePath());
                        }
                    }
                }
                
                if (itemDAO.purgeRevisions(item.getId(), keepRevisionId)) {
                    itemDAO.logAudit(itemId, keepRevisionId, currentUser.getId(), "Purge", "Purged prior revisions and cloud files");
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
