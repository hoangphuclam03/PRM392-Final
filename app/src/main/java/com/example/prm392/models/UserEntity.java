package com.example.prm392.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey(autoGenerate = true)
    public int localId;

    public String userId;       // Firestore doc ID
    public String fullName;
    public String email;
    public String password;     // (plain for now)
    public String avatarUrl;
    public long lastLogin;
}
