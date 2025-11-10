package com.example.prm392.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class TaskEntity {
    @PrimaryKey(autoGenerate = true)
    public int localId;

    public String taskId;        // Firestore doc ID
    public String projectId;     // foreign ref
    public String assignedTo;    // userId
    public String title;
    public String description;
    public String status;        // ToDo / InProgress / Done
    public String dueDate;
    public boolean isPendingSync; // true = waiting to sync
    public boolean pendingSync = false;
    public long lastSyncedAt = 0;
    public long updatedAt = System.currentTimeMillis();  // last time this record changed
}
