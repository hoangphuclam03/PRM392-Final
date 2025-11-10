package com.example.prm392.models;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "project_members",
        indices = {
                @Index(value = {"memberId"}, unique = true),
                @Index(value = {"projectId"}),
                @Index(value = {"userId"})
        }
)
public class ProjectMemberEntity {
    @PrimaryKey(autoGenerate = true)
    public int localId;

    // 🔹 unique UUID or Firestore doc ID
    public String memberId;

    // 🔹 reference to ProjectEntity.projectId & UserEntity.userId
    public String projectId;
    public String userId;

    // ✅ đồng bộ với UserEntity
    public String fullName;    // thay name → fullName
    public String role;        // e.g. "Manager", "Member"

    public boolean pendingSync = false;
    public long lastSyncedAt = 0;
    // timestamp for conflict resolution
    public long updatedAt = System.currentTimeMillis();
}
