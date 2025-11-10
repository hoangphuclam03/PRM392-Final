package com.example.prm392.models;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "chats",
        indices = {
                @Index(value = {"messageId"}, unique = true), // khóa dedupe
                @Index(value = {"chatId"}),                   // Firestore doc id (có thể null lúc đầu)
                @Index(value = {"projectId"})
        }
)
public class ChatEntity {
    @PrimaryKey(autoGenerate = true)
    public int localId;

    // ✅ UUID tạo ở client – dùng để upsert/dedupe giữa local và Firestore
    public String messageId;

    // Firestore doc ID (có sau khi add thành công)
    public String chatId;

    public String senderId;
    public String receiverId; // null = group
    public String projectId;
    public String message;
    public long   timestamp;
    public boolean isPendingSync;
}
