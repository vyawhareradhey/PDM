package com.pdm.ui.home;

import com.pdm.core.SessionContext;
import com.pdm.core.User;
import com.pdm.service.ItemService;
import com.pdm.ui.auth.LoginFrame;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.CardLayout;

public class MainFrame extends JFrame {
    
    // Class level references
    private ItemService itemService;
    private ItemTablePanel itemPanel;
    private NavigatorPanel navPanel;
    private com.pdm.ui.item.ItemDetailPanel itemDetailPanel;
    private JPanel workspaceContainer;
    private CardLayout cardLayout;

    public MainFrame() {
        super("Enterprise PDM System");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Setup Application Menu Bar
        setJMenuBar(createMenuBar());
        
        // 0. Initialize Services & Panels first
        this.itemService = new ItemService();
        this.itemPanel = new ItemTablePanel(this);
        this.navPanel = new NavigatorPanel();
        this.itemDetailPanel = new com.pdm.ui.item.ItemDetailPanel(this);
        
        // Setup Workspace CardLayout
        this.cardLayout = new CardLayout();
        this.workspaceContainer = new JPanel(cardLayout);
        this.workspaceContainer.add(itemPanel, "TABLE");
        this.workspaceContainer.add(itemDetailPanel, "DETAIL");

        // 2. Top Header Container
        // --- Siemens Style App Banner ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 15, 10, 15));
        headerPanel.setBackground(java.awt.Color.decode("#0E5A6F"));
        
        JLabel appTitle = new JLabel("PDM System");
        appTitle.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
        appTitle.setForeground(java.awt.Color.WHITE);
        headerPanel.add(appTitle, BorderLayout.WEST);
        
        User user = SessionContext.getInstance().getCurrentUser();
        String userInfo = (user != null) ? "User: " + user.getUsername() + " (" + user.getRole().getName() + ")" : "User: Unknown";
        JLabel userLabel = new JLabel(userInfo);
        userLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
        userLabel.setForeground(java.awt.Color.WHITE);
        headerPanel.add(userLabel, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.NORTH);

        // 3. Main Split Pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navPanel, workspaceContainer);
        splitPane.setDividerLocation(250);
        splitPane.setOneTouchExpandable(true);
        splitPane.setBorder(javax.swing.BorderFactory.createEmptyBorder()); // Remove native border
        
        add(splitPane, BorderLayout.CENTER);

        // Window settings
        setPreferredSize(new Dimension(1024, 768));
        pack();
        setLocationRelativeTo(null);
        
        // 4. Initial Load
        itemPanel.loadData(itemService.searchItems(""));
        
        // 5. Tree Selection Updates Table
        navPanel.addTreeSelectionListener(e -> {
            com.pdm.core.Folder selectedFolder = navPanel.getSelectedFolder();
            if (selectedFolder != null) {
                itemPanel.loadData(itemService.getItemsByFolder(selectedFolder.getId()));
            } else {
                // Root selected or nothing selected
                itemPanel.loadData(itemService.searchItems(""));
            }
        });
        
        // 6. Search Panel Integration
        navPanel.addSearchListener(e -> {
            String q = navPanel.getSearchQuery();
            if (q != null && !q.trim().isEmpty()) {
                navPanel.clearSelection();
                itemPanel.loadData(itemService.searchItems(q));
            } else {
                itemPanel.loadData(itemService.searchItems(""));
            }
        });
    }
    
    public void refreshTableData() {
        com.pdm.core.Folder folder = navPanel.getSelectedFolder();
        if (folder != null) {
            itemPanel.loadData(itemService.getItemsByFolder(folder.getId()));
        } else {
            String q = navPanel.getSearchQuery();
            if (q != null && !q.trim().isEmpty()) {
                itemPanel.loadData(itemService.searchItems(q));
            } else {
                itemPanel.loadData(itemService.searchItems(""));
            }
        }
    }

    public void showItemTable() {
        refreshTableData();
        cardLayout.show(workspaceContainer, "TABLE");
    }

    public void showItemDetails(String itemId) {
        itemDetailPanel.loadItem(itemId);
        cardLayout.show(workspaceContainer, "DETAIL");
    }

    private javax.swing.JMenuBar createMenuBar() {
        javax.swing.JMenuBar menuBar = new javax.swing.JMenuBar();
        menuBar.setBackground(java.awt.Color.decode("#E6E6E6"));
        menuBar.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.decode("#C5C5C5")));
        
        // --- File Menu ---
        javax.swing.JMenu fileMenu = new javax.swing.JMenu("File");
        javax.swing.JMenuItem openItem = new javax.swing.JMenuItem("Open Item");
        openItem.addActionListener(e -> {
             int row = itemPanel.getSelectedRow();
             if (row != -1) {
                 String itemId = itemPanel.getItemIdAt(row);
                 showItemDetails(itemId);
             } else {
                 javax.swing.JOptionPane.showMessageDialog(this, "Please select an item in the dashboard first.");
             }
        });
        
        javax.swing.JMenuItem newItem = new javax.swing.JMenuItem("New Item");
        newItem.addActionListener(e -> {
            com.pdm.ui.creation.NewItemDialog dialog = new com.pdm.ui.creation.NewItemDialog(this);
            dialog.setVisible(true);
            if (dialog.isCreated()) {
                itemPanel.loadData(itemService.searchItems(""));
            }
        });
        
        javax.swing.JMenuItem newFolderItem = new javax.swing.JMenuItem("New Folder");
        newFolderItem.addActionListener(e -> {
             String name = javax.swing.JOptionPane.showInputDialog(this, "Enter Folder Name:");
             if (name != null && !name.trim().isEmpty()) {
                 com.pdm.core.Folder parent = navPanel.getSelectedFolder();
                 com.pdm.service.FolderService fs = new com.pdm.service.FolderService();
                 if (fs.createFolder(name, parent)) {
                     navPanel.refreshTree();
                 } else {
                     javax.swing.JOptionPane.showMessageDialog(this, "Failed to create folder.");
                 }
             }
        });

        javax.swing.JMenuItem reviseItem = new javax.swing.JMenuItem("Revise Item");
        reviseItem.addActionListener(e -> performLifecycleAction("REVISE"));
        
        javax.swing.JMenuItem purgeItem = new javax.swing.JMenuItem("Purge Iterations");
        purgeItem.addActionListener(e -> performLifecycleAction("PURGE"));
        
        javax.swing.JMenuItem lockItem = new javax.swing.JMenuItem("Lock Item");
        lockItem.addActionListener(e -> performLifecycleAction("LOCK"));
        
        javax.swing.JMenuItem unlockExplicit = new javax.swing.JMenuItem("Unlock Item");
        unlockExplicit.addActionListener(e -> performLifecycleAction("UNLOCK"));
        
        javax.swing.JMenuItem viewLogs = new javax.swing.JMenuItem("View Audit Logs");
        viewLogs.addActionListener(e -> {
             int row = itemPanel.getSelectedRow();
             if (row != -1) {
                 String itemId = itemPanel.getItemIdAt(row);
                 new com.pdm.ui.item.AuditLogDialog(this, itemId).setVisible(true);
             } else {
                 javax.swing.JOptionPane.showMessageDialog(this, "Please select an item first.");
             }
        });

        // RBAC Check for Menu Visibility
        boolean isPrivileged = false;
        if (SessionContext.getInstance().getCurrentUser() != null) {
            String roleName = SessionContext.getInstance().getCurrentUser().getRole().getName();
            isPrivileged = "Admin".equalsIgnoreCase(roleName) || "Manager".equalsIgnoreCase(roleName);
        }
        reviseItem.setVisible(isPrivileged);
        purgeItem.setVisible(isPrivileged);
        lockItem.setVisible(isPrivileged);
        unlockExplicit.setVisible(isPrivileged);
        viewLogs.setVisible(isPrivileged);
        
        javax.swing.JMenuItem importExportItem = new javax.swing.JMenuItem("Import / Export");
        javax.swing.JMenuItem saveItem = new javax.swing.JMenuItem("Save / Save As");
        
        fileMenu.add(openItem);
        fileMenu.add(newItem);
        fileMenu.add(newFolderItem);
        fileMenu.add(reviseItem);
        fileMenu.add(purgeItem);
        fileMenu.add(viewLogs);
        fileMenu.add(importExportItem);
        fileMenu.add(saveItem);
        
        // --- Edit Menu ---
        javax.swing.JMenu editMenu = new javax.swing.JMenu("Edit");
        
        // Let's add lock/unlock under Edit
        editMenu.add(lockItem);
        editMenu.add(unlockExplicit);
        
        javax.swing.JMenuItem renameItemMenu = new javax.swing.JMenuItem("Rename");
        renameItemMenu.addActionListener(e -> {
            int row = itemPanel.getSelectedRow();
            if (row != -1) {
                String itemId = itemPanel.getItemIdAt(row);
                String newName = javax.swing.JOptionPane.showInputDialog(this, "Enter new name for Item " + itemId + ":");
                if (newName != null && !newName.trim().isEmpty()) {
                    if (itemService.renameItem(itemId, newName)) {
                        com.pdm.core.Folder folder = navPanel.getSelectedFolder();
                        if (folder != null) {
                            itemPanel.loadData(itemService.getItemsByFolder(folder.getId()));
                        } else {
                            itemPanel.loadData(itemService.searchItems(navPanel.getSearchQuery()));
                        }
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(this, "Failed to rename Item.");
                    }
                }
            } else {
                com.pdm.core.Folder folder = navPanel.getSelectedFolder();
                if (folder != null) {
                    if ("Root".equalsIgnoreCase(folder.getName())) {
                        javax.swing.JOptionPane.showMessageDialog(this, "Cannot rename the Root system folder.");
                        return;
                    }
                    String newName = javax.swing.JOptionPane.showInputDialog(this, "Enter new name for Folder:");
                    if (newName != null && !newName.trim().isEmpty()) {
                        com.pdm.service.FolderService fs = new com.pdm.service.FolderService();
                        if (fs.renameFolder(folder.getId(), newName)) {
                            navPanel.refreshTree();
                        } else {
                            javax.swing.JOptionPane.showMessageDialog(this, "Failed to rename Folder.");
                        }
                    }
                } else {
                    javax.swing.JOptionPane.showMessageDialog(this, "Please select an Item or Folder first.");
                }
            }
        });
        
        javax.swing.JMenuItem deleteItemMenu = new javax.swing.JMenuItem("Delete");
        deleteItemMenu.addActionListener(e -> {
            int row = itemPanel.getSelectedRow();
            if (row != -1) {
                String itemId = itemPanel.getItemIdAt(row);
                int confirm = javax.swing.JOptionPane.showConfirmDialog(this, "Are you sure you want to delete Item " + itemId + "?\nThis action cannot be undone.", "Confirm Delete", javax.swing.JOptionPane.YES_NO_OPTION);
                if (confirm == javax.swing.JOptionPane.YES_OPTION) {
                    if (itemService.deleteItem(itemId)) {
                        com.pdm.core.Folder folder = navPanel.getSelectedFolder();
                        if (folder != null) {
                            itemPanel.loadData(itemService.getItemsByFolder(folder.getId()));
                        } else {
                            itemPanel.loadData(itemService.searchItems(navPanel.getSearchQuery()));
                        }
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(this, "Failed to delete Item.");
                    }
                }
            } else {
                com.pdm.core.Folder folder = navPanel.getSelectedFolder();
                if (folder != null) {
                    if ("Root".equalsIgnoreCase(folder.getName())) {
                        javax.swing.JOptionPane.showMessageDialog(this, "Cannot delete the Root system folder.");
                        return;
                    }
                    int confirm = javax.swing.JOptionPane.showConfirmDialog(this, "Are you sure you want to delete Folder '" + folder.getName() + "'?\nWARNING: All items present in this folder will also be permanently deleted.", "Confirm Delete", javax.swing.JOptionPane.YES_NO_OPTION);
                    if (confirm == javax.swing.JOptionPane.YES_OPTION) {
                        com.pdm.service.FolderService fs = new com.pdm.service.FolderService();
                        if (fs.deleteFolder(folder.getId())) {
                            navPanel.refreshTree();
                            itemPanel.loadData(itemService.searchItems(navPanel.getSearchQuery()));
                        } else {
                            javax.swing.JOptionPane.showMessageDialog(this, "Failed to delete Folder (Folders with sub-folders may need to be emptied first).");
                        }
                    }
                } else {
                    javax.swing.JOptionPane.showMessageDialog(this, "Please select an Item or Folder first.");
                }
            }
        });
        
        editMenu.add(renameItemMenu);
        editMenu.add(deleteItemMenu);
        
        // --- View Menu ---
        javax.swing.JMenu viewMenu = new javax.swing.JMenu("View");
        viewMenu.add(new javax.swing.JMenuItem("Change layout"));
        viewMenu.add(new javax.swing.JMenuItem("Show/Hide panels"));
        javax.swing.JMenuItem refreshItem = new javax.swing.JMenuItem("Refresh");
        refreshItem.addActionListener(e -> refreshTableData());
        viewMenu.add(refreshItem);
        
        // --- Insert Menu ---
        javax.swing.JMenu insertMenu = new javax.swing.JMenu("Insert");
        insertMenu.add(new javax.swing.JMenuItem("Insert Item into Structure"));
        insertMenu.add(new javax.swing.JMenuItem("Attach Dataset"));
        
        // --- Tools Menu ---
        javax.swing.JMenu toolsMenu = new javax.swing.JMenu("Tools");
        toolsMenu.add(new javax.swing.JMenuItem("Workflow Designer"));
        toolsMenu.add(new javax.swing.JMenuItem("Access Manager"));
        toolsMenu.add(new javax.swing.JMenuItem("Queries"));
        
        // --- Window Menu ---
        javax.swing.JMenu windowMenu = new javax.swing.JMenu("Window");
        windowMenu.add(new javax.swing.JMenuItem("Switch between opened objects"));
        
        // --- Help Menu ---
        javax.swing.JMenu helpMenu = new javax.swing.JMenu("Help");
        helpMenu.add(new javax.swing.JMenuItem("Documentation"));
        
        javax.swing.JMenuItem aboutItem = new javax.swing.JMenuItem("About");
        aboutItem.addActionListener(e -> {
            new com.pdm.ui.help.AboutDialog(this).setVisible(true);
        });
        helpMenu.add(aboutItem);
        
        // Assemble Menu Bar
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(insertMenu);
        menuBar.add(toolsMenu);
        menuBar.add(windowMenu);
        menuBar.add(helpMenu);
        
        menuBar.add(javax.swing.Box.createHorizontalGlue());
        javax.swing.JMenu logoutMenu = new javax.swing.JMenu("Logout");
        logoutMenu.setForeground(java.awt.Color.RED);
        logoutMenu.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                logout();
            }
        });
        menuBar.add(logoutMenu);
        
        return menuBar;
    }
    
    private void performLifecycleAction(String action) {
         int row = itemPanel.getSelectedRow();
         if (row == -1) {
             javax.swing.JOptionPane.showMessageDialog(this, "Please select an item.");
             return;
         }
         String itemId = itemPanel.getItemIdAt(row);
         String revId = itemPanel.getRevisionAt(row);
         
         boolean success = false;
         if ("CHECKOUT".equals(action)) {
             success = itemService.checkoutItem(itemId, revId);
         } else if ("CHECKIN".equals(action)) {
             // 1. Validate if file is modified
             if (itemService.isItemModified(itemId, revId)) {
                 // 2. Prompt for Commit Message
                 String commitMsg = javax.swing.JOptionPane.showInputDialog(this, "Enter Commit Message:");
                 if (commitMsg != null) { // If null, user cancelled
                     success = itemService.checkinItem(itemId, revId, commitMsg);
                 } else {
                     return; // Cancelled
                 }
             } else {
                 if (itemService.unlockItem(itemId, revId)) {
                     javax.swing.JOptionPane.showMessageDialog(this, "No changes detected. Item has been unlocked/released.");
                     success = true; // Refresh table
                 } else {
                     javax.swing.JOptionPane.showMessageDialog(this, "Operation Failed: Could not unlock item.");
                 }
                 // success check handles refresh
                 if (!success) return; 
             }
         } else if ("PROMOTE".equals(action)) {
             success = itemService.promoteItem(itemId, revId);
         } else if ("REVISE".equals(action)) {
             success = itemService.reviseItem(itemId, revId);
         } else if ("PURGE".equals(action)) {
             success = itemService.purgeItemRevisions(itemId, revId);
         } else if ("LOCK".equals(action)) {
             success = itemService.lockItem(itemId, revId);
         } else if ("UNLOCK".equals(action)) {
             success = itemService.unlockItem(itemId, revId);
         }
         
         if (success) {
             javax.swing.JOptionPane.showMessageDialog(this, "Operation Successful!");
             itemPanel.loadData(itemService.searchItems("")); // Refresh
         } else {
             javax.swing.JOptionPane.showMessageDialog(this, "Operation Failed. Possible reasons:\n- Item is locked by someone.\n- Item is already obsolete.\n- System error.");
         }
    }
    
    private void logout() {
        // Clear session
        SessionContext.getInstance().logout();
        // Close MainFrame
        dispose();
        // Re-open LoginFrame
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
