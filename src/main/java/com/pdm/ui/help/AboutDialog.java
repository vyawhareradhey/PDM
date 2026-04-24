package com.pdm.ui.help;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JEditorPane;

public class AboutDialog extends JDialog {

    public AboutDialog(JFrame parent) {
        super(parent, "About PDM System", true);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        
        String htmlContent = "<html><body style='font-family: SansSerif; font-size: 11px; padding: 15px; color: #2C2C2C;'>"
                + "<h1 style='color: #0E5A6F;'>Enterprise PDM System</h1>"
                + "<p>Welcome to the Enterprise Product Data Management (PDM) System. This application is designed to mimic professional engineering and PLM systems (like Siemens Teamcenter), offering a robust, secure, and structured environment for managing product data.</p>"
                + "<h2>Core Features</h2>"
                + "<ul>"
                + "<li><b>Secure Authentication</b>: Role-based access control separating regular Engineers from system Administrators.</li>"
                + "<li><b>Workspace Routing</b>: Integration with local OS file operations automatically moving files between secure Vault storage and active Workspaces (`~/.pdm/vault` & `~/.pdm/workspace`).</li>"
                + "<li><b>Lifecycle Management</b>: Full check-out, check-in, and locking mechanisms to prevent conflicting edits.</li>"
                + "<li><b>Item Revisions</b>: Comprehensive version control allowing users to securely Revise items and preserve historical data.</li>"
                + "<li><b>Folder Tree Synchronization</b>: Native desktop tree structure for organizing parts, documents, and assemblies seamlessly.</li>"
                + "<li><b>Advanced Multi-Search</b>: Integrated search functionality to cross-reference items by ID, Name, or Type directly from the Navigator.</li>"
                + "<li><b>Siemens Rich Client Aesthetics</b>: Custom Metal Look & Feel overrides enforcing enterprise dark teal headers, strict workspace grids, and robust structural borders.</li>"
                + "</ul>";

        JEditorPane editorPane = new JEditorPane("text/html", htmlContent);
        editorPane.setEditable(false);
        editorPane.setBackground(java.awt.Color.decode("#F4F6F8"));
        
        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(null);
        
        add(scrollPane, BorderLayout.CENTER);
        
        setSize(new Dimension(550, 450));
        setLocationRelativeTo(getParent());
    }
}
