package com.ecartes.rfid_demo.model;

public class ScanLog {
    private int logId;
    private String tagId;
    private String scanTime;
    private String location;
    private String status;

    public ScanLog(int logId, String tagId, String scanTime, String location, String status) {
        this.logId = logId;
        this.tagId = tagId;
        this.scanTime = scanTime;
        this.location = location;
        this.status = status;
    }

    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public String getScanTime() {
        return scanTime;
    }

    public void setScanTime(String scanTime) {
        this.scanTime = scanTime;
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

    @Override
    public String toString() {
        return "ScanLog{" +
                "logId=" + logId +
                ", tagId='" + tagId + '\'' +
                ", scanTime='" + scanTime + '\'' +
                ", location='" + location + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}