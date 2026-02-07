package com.ecartes.rfid_demo.model;

public class RfidTag {
    private String tagId;
    private String timestamp;
    private String status; // "Detected", "New", "Saved", "Discarded" - initially "Detected" from hardware, then updated by user
    private int count;

    public RfidTag(String tagId, String timestamp, String status) {
        this.tagId = tagId;
        this.timestamp = timestamp;
        this.status = status; // Hardware only provides tagId, status is set by user later
        this.count = 1;
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}