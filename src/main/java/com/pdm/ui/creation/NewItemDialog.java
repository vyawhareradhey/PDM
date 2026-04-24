package com.pdm.ui.creation;

import com.pdm.service.ItemService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class NewItemDialog extends JDialog {
    private JTextField nameField;
    private JComboBox<String> typeCombo;
    private JTextArea descArea;
    private JComboBox<com.pdm.core.Folder> folderCombo;
    private ItemService itemService;
    private com.pdm.service.FolderService folderService;
    private boolean isCreated = false;

    public NewItemDialog(JFrame parent) {
        super(parent, "Create New Item", true);
        this.itemService = new ItemService();
        this.folderService = new com.pdm.service.FolderService();
        initUI();
    }

    private JTextField fileField;
    private java.io.File selectedFile;

    private void initUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        mainPanel.setBackground(new Color(245, 245, 245));

        // Title
        JLabel titleLabel = new JLabel("New Item Wizard");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(20));

        // Form
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10)); // Increased rows
        formPanel.setBackground(new Color(245, 245, 245));
        formPanel.setMaximumSize(new Dimension(500, 200));
        formPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        formPanel.add(new JLabel("Type:"));
        String[] types = {"Part", "Document", "Assembly"};
        typeCombo = new JComboBox<>(types);
        formPanel.add(typeCombo);
        
        formPanel.add(new JLabel("Location:"));
        folderCombo = new JComboBox<>();
        java.util.List<com.pdm.core.Folder> folders = folderService.getAllFolders();
        for (com.pdm.core.Folder f : folders) {
            folderCombo.addItem(f);
        }
        com.pdm.core.Folder rootFolder = folderService.getRootFolder();
        if (rootFolder != null) {
            for (int i = 0; i < folderCombo.getItemCount(); i++) {
                if (folderCombo.getItemAt(i).getId() == rootFolder.getId()) {
                    folderCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        folderCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof com.pdm.core.Folder) {
                    setText(folderService.getPhysicalPath((com.pdm.core.Folder) value).replace("/Users/radheyvyawhare/Desktop/PDM/items/", ""));
                }
                return this;
            }
        });
        
        formPanel.add(folderCombo);
        
        formPanel.add(new JLabel("File (Required):"));
        JPanel filePanel = new JPanel(new BorderLayout(5, 0));
        filePanel.setBackground(new Color(245, 245, 245));
        fileField = new JTextField();
        fileField.setEditable(false);
        JButton browseBtn = new JButton("...");
        browseBtn.addActionListener(e -> chooseFile());
        filePanel.add(fileField, BorderLayout.CENTER);
        filePanel.add(browseBtn, BorderLayout.EAST);
        formPanel.add(filePanel);

        formPanel.add(new JLabel("Name:"));
        nameField = new JTextField();
        formPanel.add(nameField);

        formPanel.add(new JLabel("Description:"));
        descArea = new JTextArea(3, 20);
        descArea.setLineWrap(true);
        JScrollPane descScroll = new JScrollPane(descArea);
        formPanel.add(descScroll);

        mainPanel.add(formPanel);
        mainPanel.add(Box.createVerticalStrut(20));

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(245, 245, 245));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(500, 40));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        
        JButton createBtn = new JButton("Create");
        createBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        createBtn.addActionListener(this::handleCreate);
        
        buttonPanel.add(cancelBtn);
        buttonPanel.add(createBtn);
        
        mainPanel.add(buttonPanel);

        add(mainPanel);
        
        pack();
        setMinimumSize(new Dimension(450, 400));
        setLocationRelativeTo(getParent());
    }
    
    private void chooseFile() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        if (fc.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
            fileField.setText(selectedFile.getName());
        }
    }

    private void handleCreate(ActionEvent e) {
        String name = nameField.getText().trim();
        String type = (String) typeCombo.getSelectedItem();
        String desc = descArea.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Item Name is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this, "File is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        com.pdm.core.Folder selectedFolder = (com.pdm.core.Folder) folderCombo.getSelectedItem();
        
        if (itemService.createNewItem(name, type, desc, selectedFile, selectedFolder)) {
            JOptionPane.showMessageDialog(this, "Item created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            isCreated = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to create item.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public boolean isCreated() {
        return isCreated;
    }
}
