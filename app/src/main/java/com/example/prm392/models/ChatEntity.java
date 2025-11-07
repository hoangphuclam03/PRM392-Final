package com.example.prm392.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chats")
public class ChatEntity {
    @PrimaryKey(autoGenerate = true)
    public int localId;

    public String chatId;        // Firestore doc ID
    public String senderId;
    public String receiverId;    // for private or null for group
    public String projectId;     // optional for group chat
    public String message;
    public long timestamp;
    public boolean isPendingSync;
}
