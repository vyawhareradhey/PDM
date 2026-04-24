package com.pdm.ui.home;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class ItemTablePanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private MainFrame parentFrame;

    public ItemTablePanel(MainFrame parent) {
        this.parentFrame = parent;
        setLayout(new BorderLayout());
        
        // Column Names
        String[] columns = {"ID", "Name", "Type", "Revision", "Status", "Owner", "Checked-Out By"};
        
        // Initial Empty Data
        tableModel = new DefaultTableModel(new Object[][]{}, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel) {
            @Override
            public java.awt.Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                java.awt.Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? java.awt.Color.decode("#FFFFFF") : java.awt.Color.decode("#F5F7FA"));
                } else {
                    c.setBackground(java.awt.Color.decode("#D2E6FF")); // Light selection mimicking Siemens
                }
                c.setForeground(java.awt.Color.decode("#2C2C2C"));
                return c;
            }
        };
        table.setFillsViewportHeight(true);
        table.setRowHeight(26);
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);
        table.setGridColor(java.awt.Color.decode("#C5C5C5"));
        table.setBackground(java.awt.Color.decode("#FFFFFF"));
        table.setOpaque(true);
        
        javax.swing.table.JTableHeader header = table.getTableHeader();
        header.setBackground(java.awt.Color.decode("#E6E6E6"));
        header.setForeground(java.awt.Color.decode("#2C2C2C"));
        header.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
        header.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.decode("#C5C5C5")));
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(java.awt.Color.decode("#F4F6F8"));
        scrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.decode("#C5C5C5")));
        
        setBackground(java.awt.Color.decode("#F4F6F8"));
        add(scrollPane, BorderLayout.CENTER);
        
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent me) {
                if (me.getClickCount() == 2) {
                     JTable target = (JTable)me.getSource();
                     int row = target.getSelectedRow();
                     if (row != -1) {
                         String itemId = (String) target.getValueAt(row, 0); // Col 0 is ID
                         parentFrame.showItemDetails(itemId);
                     }
                }
            }
        });
    }
    
    public void loadData(java.util.List<com.pdm.core.ItemDTO> items) {
        tableModel.setRowCount(0); // Clear existing
        for (com.pdm.core.ItemDTO item : items) {
            tableModel.addRow(new Object[]{
                item.getItemId(),
                item.getName(),
                item.getType(),
                item.getRevision(),
                item.getStatus(),
                item.getOwner(),
                item.getCheckedOutBy() // New Column
            });
        }
    }
    
    public int getSelectedRow() {
        return table.getSelectedRow();
    }
    
    public String getItemIdAt(int row) {
        return (String) table.getValueAt(row, 0);
    }
    
    public String getRevisionAt(int row) {
        return (String) table.getValueAt(row, 3);
    }
}
