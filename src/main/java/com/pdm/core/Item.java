package com.pdm.core;

public class Item {
    private int id;
    private String itemId;
    private String name;
    private String type;
    private String description;
    private User owner;

    public Item(int id, String itemId, String name, String type, String description, User owner) {
        this.id = id;
        this.itemId = itemId;
        this.name = name;
        this.type = type;
        this.description = description;
        this.owner = owner;
    }

    public int getId() { return id; }
    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public User getOwner() { return owner; }
}
