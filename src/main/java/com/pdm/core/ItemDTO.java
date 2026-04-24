package com.pdm.core;

public class ItemDTO {
    private String itemId;
    private String name;
    private String type;
    private String revision;
    private String status;
    private String owner;
    private String checkedOutBy;
    
    public ItemDTO(String itemId, String name, String type, String revision, String status, String owner, String checkedOutBy) {
        this.itemId = itemId;
        this.name = name;
        this.type = type;
        this.revision = revision;
        this.status = status;
        this.owner = owner;
        this.checkedOutBy = checkedOutBy;
    }
    
    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getRevision() { return revision; }
    public String getStatus() { return status; }
    public String getOwner() { return owner; }
    public String getCheckedOutBy() { return checkedOutBy; }
}
