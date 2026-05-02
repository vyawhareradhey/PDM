package com.pdm.service;

import com.pdm.core.Folder;
import com.pdm.core.SessionContext;
import com.pdm.core.User;
import com.pdm.dao.FolderDAO;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FolderService {
    private FolderDAO folderDAO;

    public FolderService() {
        this.folderDAO = new FolderDAO();
    }

    public boolean createFolder(String name, Folder parent) {
        User currentUser = SessionContext.getInstance().getCurrentUser();
        if (currentUser == null) return false;
        
        Integer parentId = (parent != null && parent.getId() > 0) ? parent.getId() : null;
        
        try {
            int newFolderId = folderDAO.createFolder(name, parentId, currentUser.getId());
            if (newFolderId != -1) {
                // Cloud Object Storage does not require empty physical directories
                // Logical Folder is created successfully in DB
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Folder getFolderById(int id) {
        try {
            return folderDAO.getFolderById(id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /////////////////////////////////////////////////
    //
    //item folder location
    //
    //////////////////////////////////////////////////
    public String getPhysicalPath(Folder folder) {
        String base = "items";
        if (folder == null) return base + "/Root";
        
        java.util.List<String> parts = new java.util.ArrayList<>();
        Folder current = folder;
        while (current != null) {
            parts.add(0, current.getName());
            if (current.getParentId() > 0) {
               current = getFolderById(current.getParentId()); 
            } else {
               current = null; // Reached Root/Top
            }
        }
        
        String path = base;
        for (String p : parts) {
            if (!p.equalsIgnoreCase("Root")) {
                path += "/" + p;
            } else {
                path += "/Root";
            }
        }
        // Deduplicate duplicate Roots natively if the tree spans multiple 'Root' literals
        path = path.replace("/items/Root/Root", "/items/Root");
        return path;
    }

    public List<Folder> getFolderStructure() {
        try {
            // MVP: Eager loading (ok for small data)
            // 1. Get Roots
            List<Folder> roots = folderDAO.getRootFolders();
            for (Folder root : roots) {
                loadRecursive(root);
            }
            return roots;
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    private void loadRecursive(Folder parent) throws SQLException {
        List<Folder> subs = folderDAO.getSubFolders(parent.getId());
        for (Folder sub : subs) {
            parent.addSubFolder(sub);
            loadRecursive(sub);
        }
    }

    public Folder getRootFolder() {
        try {
            List<Folder> roots = folderDAO.getRootFolders();
            for (Folder f : roots) {
                if ("Root".equalsIgnoreCase(f.getName())) {
                    return f;
                }
            }
            if (!roots.isEmpty()) {
                return roots.get(0); // fallback
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Folder> getAllFolders() {
        List<Folder> result = new ArrayList<>();
        List<Folder> roots = getFolderStructure();
        flattenFolders(roots, result);
        return result;
    }
    
    private void flattenFolders(List<Folder> list, List<Folder> result) {
        for (Folder f : list) {
            result.add(f);
            flattenFolders(f.getSubFolders(), result);
        }
    }

    public boolean renameFolder(int folderId, String newName) {
        if (newName == null || newName.trim().isEmpty()) return false;
        try {
            return folderDAO.renameFolder(folderId, newName.trim());
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteFolder(int folderId) {
        try {
            com.pdm.dao.ItemDAO itemDAO = new com.pdm.dao.ItemDAO();
            List<com.pdm.core.ItemDTO> items = itemDAO.getItemsByFolder(folderId);
            for (com.pdm.core.ItemDTO item : items) {
                itemDAO.deleteItem(item.getItemId());
            }
            return folderDAO.deleteFolder(folderId);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
