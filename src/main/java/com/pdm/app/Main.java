package com.pdm.app;

import com.pdm.ui.auth.LoginFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        // Setup Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            System.setProperty("sun.java2d.uiScale", "1.0");
            
            // Apply global font
            java.awt.Font baseFont = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11);
            javax.swing.plaf.FontUIResource fontRes = new javax.swing.plaf.FontUIResource(baseFont);
            java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = UIManager.get(key);
                if (value instanceof javax.swing.plaf.FontUIResource) {
                    UIManager.put(key, fontRes);
                }
            }
            
            // Clean up some metal defaults
            UIManager.put("SplitPane.background", java.awt.Color.decode("#C5C5C5"));
            UIManager.put("SplitPane.dividerSize", 3);
            UIManager.put("Tree.expandedIcon", new javax.swing.ImageIcon(new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_ARGB))); // hide default handles if wanted, but standard is okay.
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}
