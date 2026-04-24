package com.pdm.ui.item;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import com.pdm.dao.DatabaseManager;

public class AuditLogDialog extends JDialog {
    
    private JTable logsTable;
    private String itemId;

    public AuditLogDialog(JFrame parent, String itemId) {
        super(parent, "Audit Logs - " + itemId, true);
        this.itemId = itemId;
        initUI();
        loadLogs();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        logsTable = new JTable();
        add(new JScrollPane(logsTable), BorderLayout.CENTER);
        setSize(new Dimension(800, 400));
        setLocationRelativeTo(getParent());
    }

    private void loadLogs() {
        String sql = "SELECT l.item_id, l.revision_id, u.username, l.action, l.timestamp, l.details " +
                     "FROM item_audit_logs l " +
                     "JOIN users u ON l.user_id = u.id " +
                     "WHERE l.item_id = ? " +
                     "ORDER BY l.timestamp DESC";
                     
        Vector<String> columns = new Vector<>();
        columns.add("Item ID");
        columns.add("Revision");
        columns.add("User");
        columns.add("Action");
        columns.add("Timestamp");
        columns.add("Details");
        
        Vector<Vector<String>> data = new Vector<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Vector<String> row = new Vector<>();
                    row.add(rs.getString("item_id"));
                    row.add(rs.getString("revision_id"));
                    row.add(rs.getString("username"));
                    row.add(rs.getString("action"));
                    row.add(rs.getTimestamp("timestamp").toString());
                    row.add(rs.getString("details"));
                    data.add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        logsTable.setModel(new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
    }
}
