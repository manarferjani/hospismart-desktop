package com.hospismart.hospismartdesktop.models;

import java.time.LocalDateTime;

public class Notification {
    private int id;
    private String content;
    private LocalDateTime createdAt;
    private boolean isRead;
    private int userId;
    private String type;
    private String linkUrl;

    public Notification() {
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }
}
