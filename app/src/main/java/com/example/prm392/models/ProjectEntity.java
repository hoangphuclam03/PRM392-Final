package com.example.prm392.models;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "projects",
        indices = {
                @Index(value = {"projectId"}, unique = true),
                @Index(value = {"createdBy"}),
                @Index(value = {"ownerId"})
        }
)
public class ProjectEntity {
    @PrimaryKey(autoGenerate = true)
    public int localId;

    // Firestore doc ID (unique)
    public String projectId;

    public String projectName;
    public String description;

    // userId of creator/owner (không dùng FK cứng để tránh lỗi sync offline)
    public String createdBy;
    public String createdAt;    // ISO string (e.g., 2025-11-08T10:00:00Z)
    public String ownerId;

    public long updatedAt;      // epoch millis
    public boolean isPublic;
    public boolean pendingSync = false;
    public long lastSyncedAt = 0;
}
