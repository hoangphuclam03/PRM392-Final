// models/ChatroomModel.java
package com.example.prm392.models;

public class ChatroomModel {
    public String chatroomId;
    public String otherUserId;
    public String lastMessage;
    public String lastMessageSenderId;
    public long lastMessageTimestamp;

    public ChatroomModel(String chatroomId, String otherUserId,
                         String lastMessage, String lastMessageSenderId, long lastMessageTimestamp) {
        this.chatroomId = chatroomId;
        this.otherUserId = otherUserId;
        this.lastMessage = lastMessage;
        this.lastMessageSenderId = lastMessageSenderId;
        this.lastMessageTimestamp = lastMessageTimestamp;
    }
}
