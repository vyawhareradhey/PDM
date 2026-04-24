package com.pdm.core;

import java.util.List;
import java.util.ArrayList;

public class Folder {
    private int id;
    private String name;
    private int parentId; // 0 if root
    private int ownerId;
    private List<Folder> subFolders;
    private List<ItemDTO> items; // For UI display

    public Folder(int id, String name, int parentId, int ownerId) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.ownerId = ownerId;
        this.subFolders = new ArrayList<>();
        this.items = new ArrayList<>();
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getParentId() { return parentId; }
    public int getOwnerId() { return ownerId; }

    public List<Folder> getSubFolders() { return subFolders; }
    public void addSubFolder(Folder folder) { this.subFolders.add(folder); }

    public List<ItemDTO> getItems() { return items; }
    public void addItem(ItemDTO item) { this.items.add(item); }
    
    @Override
    public String toString() { return name; } // For JTree display
}
