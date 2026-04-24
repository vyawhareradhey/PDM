package com.pdm.ui.search;

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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SearchDialog extends JDialog {
    private JTextField queryField;
    private String searchQuery = null;
    private boolean approved = false;

    public SearchDialog(JFrame parent) {
        super(parent, "Search Items", true);
        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        mainPanel.setBackground(new Color(245, 245, 245));

        JLabel titleLabel = new JLabel("Search Criteria");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(20));

        JPanel formPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        formPanel.setBackground(new Color(245, 245, 245));
        formPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        formPanel.add(new JLabel("ID / Name / Type:"));
        queryField = new JTextField();
        formPanel.add(queryField);
        
        mainPanel.add(formPanel);
        mainPanel.add(Box.createVerticalStrut(20));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(245, 245, 245));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        
        JButton searchBtn = new JButton("Search");
        searchBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        searchBtn.addActionListener(this::handleSearch);
        
        buttonPanel.add(cancelBtn);
        buttonPanel.add(searchBtn);
        
        mainPanel.add(buttonPanel);

        add(mainPanel);
        
        pack();
        setMinimumSize(new Dimension(400, 200));
        setLocationRelativeTo(getParent());
    }

    private void handleSearch(ActionEvent e) {
        this.searchQuery = queryField.getText();
        this.approved = true;
        dispose();
    }

    public boolean isApproved() {
        return approved;
    }

    public String getSearchQuery() {
        return searchQuery;
    }
}
