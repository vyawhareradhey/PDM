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
import javax.swing.SwingConstants;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private AuthService authService;

    public LoginFrame() {
        super("PDM System Login");
        this.authService = new AuthService();
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Main container with padding
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        mainPanel.setBackground(new Color(245, 245, 245)); // Light gray background

        // Title
        JLabel titleLabel = new JLabel("Enterprise PDM");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setForeground(new Color(50, 50, 50)); // Dark gray
        
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(30));

        // Form Panel
        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        formPanel.setBackground(new Color(245, 245, 245));
        formPanel.setMaximumSize(new Dimension(300, 80));
        
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        usernameField = new JTextField();
        usernameField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        formPanel.add(userLabel);
        formPanel.add(usernameField);
        formPanel.add(passLabel);
        formPanel.add(passwordField);
        
        mainPanel.add(formPanel);
        mainPanel.add(Box.createVerticalStrut(20));

        // Login Button
        JButton loginButton = new JButton("Login");
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.addActionListener(this::handleLogin);
        
        mainPanel.add(loginButton);
        mainPanel.add(Box.createVerticalStrut(10));
        
        // Links Panel
        JPanel linksPanel = new JPanel();
        linksPanel.setBackground(new Color(245, 245, 245));
        
        JButton registerBtn = new JButton("Register");
        registerBtn.setBorderPainted(false);
        registerBtn.setForeground(Color.BLUE);
        registerBtn.addActionListener(e -> new RegisterFrame().setVisible(true));
        
        JButton forgotBtn = new JButton("Forgot Password?");
        forgotBtn.setBorderPainted(false);
        forgotBtn.setForeground(Color.RED);
        forgotBtn.addActionListener(e -> new ForgotPasswordDialog(this).setVisible(true));
        
        linksPanel.add(registerBtn);
        linksPanel.add(new JLabel("|"));
        linksPanel.add(forgotBtn);
        
        mainPanel.add(linksPanel);

        add(mainPanel, BorderLayout.CENTER);

        // Required Window Rules
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    private void handleLogin(ActionEvent e) {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (authService.login(username, password)) {
            // Login successful
            // dispose logic frame and open main window
            dispose();
            javax.swing.SwingUtilities.invokeLater(() -> {
                new com.pdm.ui.home.MainFrame().setVisible(true);
            });
        } else {
            JOptionPane.showMessageDialog(this, "Invalid credentials", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
