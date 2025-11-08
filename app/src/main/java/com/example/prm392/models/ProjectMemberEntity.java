package com.example.prm392.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "project_members")
public class ProjectMemberEntity {

    @PrimaryKey(autoGenerate = true)
    public int localId;

    public String memberId;   // 🔹 unique UUID or Firestore doc ID
    public String projectId;  // 🔹 FK to ProjectEntity.projectId
    public String userId;     // 🔹 FK to UserEntity.userId
    public String name;       // 🔹 cached display name (optional but useful)
    public String role;       // e.g., "Manager", "Member"
    public boolean pendingSync = false;

    // optional: timestamp for ordering / conflict resolution
    public long updatedAt = System.currentTimeMillis();
}
