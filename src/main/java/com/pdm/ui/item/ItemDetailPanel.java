package com.pdm.ui.item;

import com.pdm.core.Item;
import com.pdm.core.ItemRevision;
import com.pdm.dao.ItemDAO;
import com.pdm.ui.home.MainFrame;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class ItemDetailPanel extends JPanel {
    private String itemId;
    private ItemDAO itemDAO;
    private com.pdm.service.ItemService itemService;
    private Item item;
    private MainFrame parentFrame;

    // UI Fields
    private JTextField idField;
    private JTextField nameField;
    private JTextField typeField;
    private JTextField ownerField;
    private JTextField descField;
    private JLabel titleLabel;
    
    // UI Panels for tabs to refresh
    private JPanel revPanel;
    private JPanel histPanel;

    public ItemDetailPanel(MainFrame parent) {
        this.parentFrame = parent;
        this.itemDAO = new ItemDAO();
        this.itemService = new com.pdm.service.ItemService();
        initUI();
    }

    public void loadItem(String newItemId) {
        this.itemId = newItemId;
        loadData();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(Color.decode("#F4F6F8"));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.decode("#E6E6E6"));
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.decode("#C5C5C5")));
        
        JPanel titlePanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 10));
        titlePanel.setBackground(Color.decode("#E6E6E6"));
        
        JButton backBtn = new JButton("⬅ Back to List");
        backBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        backBtn.setFocusPainted(false);
        backBtn.addActionListener(e -> parentFrame.showItemTable());
        
        titleLabel = new JLabel("Item Details");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLabel.setForeground(Color.decode("#2C2C2C"));
        
        titlePanel.add(backBtn);
        titlePanel.add(Box.createHorizontalStrut(10));
        titlePanel.add(titleLabel);
        
        headerPanel.add(titlePanel, BorderLayout.WEST);
        
        JPanel actionPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 10));
        actionPanel.setBackground(Color.decode("#E6E6E6"));
        
        JButton btnCheckout = new JButton("Check Out");
        btnCheckout.setFocusPainted(false);
        btnCheckout.addActionListener(e -> performCheckout());
        
        JButton btnCheckin = new JButton("Check In");
        btnCheckin.setFocusPainted(false);
        btnCheckin.addActionListener(e -> performCheckin());
        
        actionPanel.add(btnCheckout);
        actionPanel.add(btnCheckin);
        
        headerPanel.add(actionPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 12));
        
        tabs.addTab("Properties", createPropertiesPanel());
        
        revPanel = createRevisionsPanel();
        tabs.addTab("Current Revisions", revPanel);
        
        histPanel = createHistoryPanel();
        tabs.addTab("Version History", histPanel);
        
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel createPropertiesPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.decode("#F4F6F8"));

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 10));
        form.setMaximumSize(new Dimension(600, 200));
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.setBackground(Color.decode("#F4F6F8"));

        idField = createReadOnlyField();
        nameField = createReadOnlyField();
        typeField = createReadOnlyField();
        ownerField = createReadOnlyField();
        descField = createReadOnlyField();

        form.add(createPropLabel("Item ID:"));
        form.add(idField);
        form.add(createPropLabel("Name:"));
        form.add(nameField);
        form.add(createPropLabel("Type:"));
        form.add(typeField);
        form.add(createPropLabel("Owner:"));
        form.add(ownerField);
        form.add(createPropLabel("Description:"));
        form.add(descField);

        panel.add(form);
        panel.add(Box.createVerticalGlue());
        return panel;
    }
    
    private JLabel createPropLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        return l;
    }
    
    private JTextField createReadOnlyField() {
        JTextField tf = new JTextField();
        tf.setEditable(false);
        tf.setBackground(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.decode("#C5C5C5")),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        return tf;
    }

    private JPanel createRevisionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] cols = {"Revision", "Status", "ID"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        styleTable(table);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] cols = {"Version Number", "File Name", "Modified By", "Date", "Commit Message"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        styleTable(table);
        table.getColumnModel().getColumn(4).setPreferredWidth(300);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }
    
    private void styleTable(JTable table) {
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setRowHeight(24);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(Color.decode("#E6E6E6"));
        table.setGridColor(Color.decode("#C5C5C5"));
        table.setSelectionBackground(Color.decode("#D2E6FF"));
    }

    private void loadData() {
        if (itemId == null || itemId.isEmpty()) return;
        try {
            this.item = itemDAO.getItemByItemId(itemId);
            if (this.item != null) {
                titleLabel.setText("Item: " + item.getItemId());
                idField.setText(item.getItemId());
                nameField.setText(item.getName());
                typeField.setText(item.getType());
                ownerField.setText(item.getOwner().getUsername());
                descField.setText(item.getDescription());
                
                List<ItemRevision> revs = itemDAO.getRevisions(item.getId(), item);
                
                // Revisions
                JScrollPane spRev = (JScrollPane) revPanel.getComponent(0);
                JTable tableRev = (JTable) spRev.getViewport().getView();
                DefaultTableModel modelRev = (DefaultTableModel) tableRev.getModel();
                modelRev.setRowCount(0);
                
                for (ItemRevision rev : revs) {
                    modelRev.addRow(new Object[]{rev.getRevisionId(), rev.getStatus(), rev.getId()});
                }
                
                // History
                JScrollPane spHist = (JScrollPane) histPanel.getComponent(0);
                JTable tableHist = (JTable) spHist.getViewport().getView();
                DefaultTableModel modelHist = (DefaultTableModel) tableHist.getModel();
                modelHist.setRowCount(0);
                
                for (int i = revs.size() - 1; i >= 0; i--) {
                    ItemRevision rev = revs.get(i);
                    int versionNumber = i + 1;
                    String modUser = rev.getModifiedByName();
                    if (modUser == null) modUser = "User " + rev.getModifiedBy();
                    
                    modelHist.addRow(new Object[]{
                        versionNumber,
                        rev.getFileName(),
                        modUser, 
                        (rev.getFileModTimestamp() != null ? rev.getFileModTimestamp().toString() : ""),
                        rev.getCommitMessage()
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void performCheckout() {
        if (item == null) return;
        try {
            java.util.List<ItemRevision> revs = itemDAO.getRevisions(item.getId(), item);
            if (revs.isEmpty()) return;
            ItemRevision latestRev = revs.get(revs.size() - 1);
            
            if (itemService.checkoutItem(itemId, latestRev.getRevisionId())) {
                JOptionPane.showMessageDialog(this, "Item Checked Out Successfully!");
                loadData();
            } else {
                JOptionPane.showMessageDialog(this, "Operation Failed. Item might be locked or you don't have access.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void performCheckin() {
        if (item == null) return;
        try {
            java.util.List<ItemRevision> revs = itemDAO.getRevisions(item.getId(), item);
            if (revs.isEmpty()) return;
            ItemRevision latestRev = revs.get(revs.size() - 1);
            
            if (itemService.isItemModified(itemId, latestRev.getRevisionId())) {
                String commitMsg = JOptionPane.showInputDialog(this, "Enter Commit Message:");
                if (commitMsg != null) {
                    if (itemService.checkinItem(itemId, latestRev.getRevisionId(), commitMsg)) {
                        JOptionPane.showMessageDialog(this, "Item Checked In Successfully!");
                        loadData();
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to check in item.");
                    }
                }
            } else {
                if (itemService.unlockItem(itemId, latestRev.getRevisionId())) {
                    JOptionPane.showMessageDialog(this, "No changes detected. Item has been unlocked/released.");
                    loadData();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to unlock item.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
