package com.pdm.ui.auth;

import com.pdm.service.AuthService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class RegisterFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField fullNameField;
    private JTextField emailField;
    private JTextField employeeIdField;
    private javax.swing.JComboBox<String> roleCombo;
    private AuthService authService;

    public RegisterFrame() {
        super("PDM System - Register");
        this.authService = new AuthService();
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        mainPanel.setBackground(new Color(245, 245, 245));

        JLabel titleLabel = new JLabel("Create Account");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(20));

        JPanel formPanel = new JPanel(new GridLayout(7, 2, 10, 10)); // Updated rows to 7
        formPanel.setBackground(new Color(245, 245, 245));

        formPanel.add(new JLabel("Full Name:"));
        fullNameField = new JTextField();
        formPanel.add(fullNameField);
        
        formPanel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        formPanel.add(usernameField);
        
        formPanel.add(new JLabel("Email:"));
        emailField = new JTextField();
        formPanel.add(emailField);

        formPanel.add(new JLabel("Employee ID:"));
        employeeIdField = new JTextField();
        formPanel.add(employeeIdField);
        
        formPanel.add(new JLabel("Role:"));
        String[] roles = {"Engineer", "Manager", "Admin"};
        roleCombo = new javax.swing.JComboBox<>(roles);
        formPanel.add(roleCombo);

        formPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        formPanel.add(passwordField);

        formPanel.add(new JLabel("Confirm Password:"));
        confirmPasswordField = new JPasswordField();
        formPanel.add(confirmPasswordField);

        mainPanel.add(formPanel);
        mainPanel.add(Box.createVerticalStrut(20));

        JButton registerButton = new JButton("Register");
        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerButton.addActionListener(this::handleRegister);
        
        mainPanel.add(registerButton);

        add(mainPanel, BorderLayout.CENTER);
        
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    private void handleRegister(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmPasswordField.getPassword());
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String employeeId = employeeIdField.getText().trim();
        String roleName = (String) roleCombo.getSelectedItem();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty() || employeeId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (authService.register(username, password, fullName, email, employeeId, roleName)) {
            JOptionPane.showMessageDialog(this, "Registration Successful! Please login.", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Registration Failed. Username might already exist.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
