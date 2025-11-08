package com.example.prm392.models;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "users",
        indices = {
                @Index(value = {"userId"}, unique = true),
                @Index(value = {"email"}, unique = true)
        }
)
public class UserEntity {
    @PrimaryKey(autoGenerate = true)
    public int localId;

    // Firestore doc ID (unique)
    public String userId;

    public String fullName;
    public String email;
    public String password;     // (plain for now)
    public String avatarUrl;

    // last successful login epoch millis
    public long lastLogin;
}
