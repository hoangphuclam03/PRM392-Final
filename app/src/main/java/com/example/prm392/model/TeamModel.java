package com.example.prm392.model;

import com.google.firebase.Timestamp;
import java.util.List;

public class TeamModel {
    private String id;
    private String name;
    private String ownerId;
    private List<String> members;
    private String lastMessage;
    private Timestamp lastMessageTimestamp;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public TeamModel() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public List<String> getMembers() { return members; }
}
