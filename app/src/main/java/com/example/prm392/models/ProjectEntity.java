package com.example.prm392.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "projects")
public class ProjectEntity {
    @PrimaryKey(autoGenerate = true)
    public int localId;

    public String projectId;     // Firestore doc ID
    public String projectName;
    public String description;
    public String createdBy;     // userId
    public String createdAt;
    public boolean isPublic;
}
