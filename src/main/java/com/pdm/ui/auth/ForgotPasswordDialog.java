package com.pdm.ui.auth;

import com.pdm.service.AuthService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class ForgotPasswordDialog extends JDialog {
    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField newPasswordField;
    private AuthService authService;

    public ForgotPasswordDialog(JFrame parent) {
        super(parent, "Reset Password", true);
        this.authService = new AuthService();
        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        mainPanel.setBackground(new Color(245, 245, 245));

        JLabel titleLabel = new JLabel("Reset Password");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(20));

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBackground(new Color(245, 245, 245));
        
        formPanel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        formPanel.add(usernameField);
        
        formPanel.add(new JLabel("Email:"));
        emailField = new JTextField();
        formPanel.add(emailField);

        formPanel.add(new JLabel("New Password:"));
        newPasswordField = new JPasswordField();
        formPanel.add(newPasswordField);

        mainPanel.add(formPanel);
        mainPanel.add(Box.createVerticalStrut(20));

        JButton resetButton = new JButton("Reset Password");
        resetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetButton.addActionListener(this::handleReset);
        
        mainPanel.add(resetButton);

        add(mainPanel, BorderLayout.CENTER);
        
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(getParent());
    }

    private void handleReset(ActionEvent e) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String newPass = new String(newPasswordField.getPassword());

        if (username.isEmpty() || email.isEmpty() || newPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (authService.resetPassword(username, email, newPass)) {
            JOptionPane.showMessageDialog(this, "Password updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Verification failed or user not found.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
