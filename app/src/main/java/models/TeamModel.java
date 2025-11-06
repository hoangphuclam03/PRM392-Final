package models;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TeamModel {
    private String name;
    private String ownerId;                   // UID owner
    private List<String> members;             // UID list
    private String lastMessage;
    private Timestamp lastMessageTimestamp;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public TeamModel() { this.members = new ArrayList<>(); }

    public TeamModel(String name, String ownerId, List<String> members,
                     String lastMessage, Timestamp lastMessageTimestamp,
                     Timestamp createdAt, Timestamp updatedAt) {
        this.name = name;
        this.ownerId = ownerId;
        this.members = (members != null) ? members : new ArrayList<>();
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public List<String> getMembers() {
        if (members == null) members = new ArrayList<>();
        return members;
    }
    public void setMembers(List<String> members) {
        this.members = (members != null) ? members : new ArrayList<>();
    }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public Timestamp getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(Timestamp t) { this.lastMessageTimestamp = t; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
