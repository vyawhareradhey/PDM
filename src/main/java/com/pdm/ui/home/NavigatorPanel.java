package com.pdm.ui.home;

import com.pdm.core.Folder;
import com.pdm.service.FolderService;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class NavigatorPanel extends JPanel {
    private JTree tree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private FolderService folderService;
    private javax.swing.JTextField searchField;
    private javax.swing.JButton searchBtn;

    public NavigatorPanel() {
        this.folderService = new FolderService();
        setLayout(new BorderLayout());
        setBackground(java.awt.Color.decode("#1F6F8B"));
        
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        searchPanel.setBackground(java.awt.Color.decode("#1F6F8B"));
        searchField = new javax.swing.JTextField();
        searchField.setBackground(java.awt.Color.WHITE);
        searchField.setForeground(java.awt.Color.decode("#2C2C2C"));
        searchField.setToolTipText("Search Items...");
        searchBtn = new javax.swing.JButton("\uD83D\uDD0D"); // Magnifying glass emoji/unicode
        searchBtn.setToolTipText("Search");
        searchBtn.setMargin(new java.awt.Insets(0, 0, 0, 0)); // Make it compact
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchBtn, BorderLayout.EAST);
        
        add(searchPanel, BorderLayout.NORTH);
        
        rootNode = new DefaultMutableTreeNode("Root");
        treeModel = new DefaultTreeModel(rootNode);
        tree = new JTree(treeModel);
        tree.setBackground(java.awt.Color.decode("#1F6F8B"));
        
        javax.swing.tree.DefaultTreeCellRenderer renderer = new javax.swing.tree.DefaultTreeCellRenderer() {
            @Override
            public java.awt.Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                java.awt.Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
                return c;
            }
        };
        renderer.setBackgroundNonSelectionColor(java.awt.Color.decode("#1F6F8B"));
        renderer.setTextNonSelectionColor(java.awt.Color.WHITE);
        renderer.setBackgroundSelectionColor(java.awt.Color.decode("#3A8FB7"));
        renderer.setTextSelectionColor(java.awt.Color.WHITE);
        renderer.setBorderSelectionColor(java.awt.Color.decode("#3A8FB7"));
        
        // Ensure PLM style folder icons (using default metal icons but clean)
        renderer.setClosedIcon(javax.swing.UIManager.getIcon("Tree.closedIcon"));
        renderer.setOpenIcon(javax.swing.UIManager.getIcon("Tree.openIcon"));
        renderer.setLeafIcon(javax.swing.UIManager.getIcon("Tree.closedIcon")); // Folders don't have files under them in navigator
        
        tree.setCellRenderer(renderer);
        
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(java.awt.Color.decode("#1F6F8B"));
        add(scrollPane, BorderLayout.CENTER);
        
        refreshTree();
    }
    
    public void refreshTree() {
        rootNode.removeAllChildren();
        
        // Load from DB
        List<Folder> roots = folderService.getFolderStructure();
        for (Folder f : roots) {
            addFolderNode(rootNode, f);
        }
        
        treeModel.reload();
        // Expand root by default
        tree.expandRow(0);
    }
    
    private void addFolderNode(DefaultMutableTreeNode parentNode, Folder folder) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(folder);
        parentNode.add(node);
        
        for (Folder sub : folder.getSubFolders()) {
            addFolderNode(node, sub);
        }
    }
    
    public Folder getSelectedFolder() {
        javax.swing.tree.TreePath path = tree.getSelectionPath();
        if (path != null) {
             Object nodeObj = path.getLastPathComponent();
             if (nodeObj instanceof DefaultMutableTreeNode) {
                 Object userObj = ((DefaultMutableTreeNode)nodeObj).getUserObject();
                 if (userObj instanceof Folder) {
                     return (Folder) userObj;
                 }
             }
        }
        return null; // Root or nothing
    }

    public void addTreeSelectionListener(javax.swing.event.TreeSelectionListener listener) {
        tree.addTreeSelectionListener(listener);
    }
    
    public void addSearchListener(java.awt.event.ActionListener listener) {
        searchField.addActionListener(listener);
        searchBtn.addActionListener(listener);
    }
    
    public String getSearchQuery() {
        return searchField.getText();
    }
    
    public void clearSelection() {
        tree.clearSelection();
    }
}
