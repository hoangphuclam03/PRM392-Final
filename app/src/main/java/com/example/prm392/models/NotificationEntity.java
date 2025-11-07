package com.example.prm392.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notifications")
public class NotificationEntity {
    @PrimaryKey(autoGenerate = true)
    public int localId;

    public String notifId;        // Firestore doc ID
    public String userId;         // recipient
    public String title;
    public String body;
    public long timestamp;
    public boolean isRead;
}
