package com.ecartes.rfid_demo.model;

public class ScanRequest {
    private String tagId;
    private String location;
    private String status;
    
    public ScanRequest(String tagId, String location, String status) {
        this.tagId = tagId;
        this.location = location;
        this.status = status;
    }
    
    public String getTagId() {
        return tagId;
    }
    
    public void setTagId(String tagId) {
        this.tagId = tagId;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}