package com.pdm.core;

import java.sql.Timestamp;

public class ItemRevision {
    private int id;
    private Item item;
    private String revisionId;
    private String status;
    private String fileName;
    private String storagePath;
    
    private Timestamp createdAt;
    private int createdBy;
    private String createdByName;
    
    private Timestamp modifiedAt;
    private int modifiedBy;
    private String modifiedByName;
    
    private Timestamp fileModTimestamp;
    private String commitMessage;

    public ItemRevision(int id, Item item, String revisionId, String status) {
        this(id, item, revisionId, status, null, null, null, 0, null, null, 0, null, null, null);
    }

    public ItemRevision(int id, Item item, String revisionId, String status, String fileName, String storagePath) {
        this(id, item, revisionId, status, fileName, storagePath, null, 0, null, null, 0, null, null, null);
    }
    
    public ItemRevision(int id, Item item, String revisionId, String status, String fileName, String storagePath,
                        Timestamp createdAt, int createdBy, String createdByName, 
                        Timestamp modifiedAt, int modifiedBy, String modifiedByName, 
                        Timestamp fileModTimestamp, String commitMessage) {
        this.id = id;
        this.item = item;
        this.revisionId = revisionId;
        this.status = status;
        this.fileName = fileName;
        this.storagePath = storagePath;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.modifiedAt = modifiedAt;
        this.modifiedBy = modifiedBy;
        this.modifiedByName = modifiedByName;
        this.fileModTimestamp = fileModTimestamp;
        this.commitMessage = commitMessage;
    }

    public int getId() { return id; }
    public Item getItem() { return item; }
    public String getRevisionId() { return revisionId; }
    public String getStatus() { return status; }
    public String getFileName() { return fileName; }
    public String getStoragePath() { return storagePath; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public int getCreatedBy() { return createdBy; }
    public String getCreatedByName() { return createdByName; }
    
    public Timestamp getModifiedAt() { return modifiedAt; }
    public int getModifiedBy() { return modifiedBy; }
    public String getModifiedByName() { return modifiedByName; }
    
    public Timestamp getFileModTimestamp() { return fileModTimestamp; }
    public void setFileModTimestamp(Timestamp ts) { this.fileModTimestamp = ts; }
    
    public String getCommitMessage() { return commitMessage; }
    public void setCommitMessage(String msg) { this.commitMessage = msg; }
    
    public void setCreatedBy(int userId) { this.createdBy = userId; }
    public void setModifiedBy(int userId) { this.modifiedBy = userId; }
}
