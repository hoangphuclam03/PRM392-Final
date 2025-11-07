package com.example.prm392.utils;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Unified Firebase utility helper
 * Supports: Auth, Firestore (Users, Projects, Tasks, Teams, Chatrooms), and Storage.
 */
public class FirebaseUtil {

    // ------------------------- 🔐 AUTH + FIRESTORE CORE -------------------------
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static FirebaseAuth getAuth() {
        return auth;
    }

    public static FirebaseFirestore getDatabase() {
        return db;
    }

    // ------------------------- 👥 USERS -------------------------
    public static CollectionReference usersCollection() {
        return db.collection("Users");
    }

    public static DocumentReference currentUserDetails() {
        String uid = (auth.getCurrentUser() != null)
                ? auth.getCurrentUser().getUid()
                : "guest_user";
        return usersCollection().document(uid);
    }

    public static String currentUserId() {
        return (auth.getCurrentUser() != null)
                ? auth.getCurrentUser().getUid()
                : "guest_user";
    }

    public static boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public static void logout() {
        auth.signOut();
    }

    // ------------------------- 🗂️ PROJECTS -------------------------
    public static CollectionReference projectsCollection() {
        return db.collection("Projects");
    }

    public static DocumentReference projectRef(String projectId) {
        return projectsCollection().document(projectId);
    }

    // ------------------------- ✅ TASKS -------------------------
    public static CollectionReference tasksCollection() {
        return db.collection("Tasks");
    }

    public static DocumentReference taskRef(String taskId) {
        return tasksCollection().document(taskId);
    }

    // ------------------------- 🧑‍🤝‍🧑 TEAMS -------------------------
    public static CollectionReference teamsCollection() {
        return db.collection("teams");
    }

    public static DocumentReference teamRef(String teamId) {
        return teamsCollection().document(teamId);
    }

    public static CollectionReference teamMessagesRef(String teamId) {
        return teamRef(teamId).collection("messages");
    }

    // ------------------------- 💬 CHATROOMS -------------------------
    public static CollectionReference allChatroomsCollection() {
        return db.collection("chatrooms");
    }

    public static DocumentReference chatroomRef(String chatroomId) {
        return allChatroomsCollection().document(chatroomId);
    }

    public static CollectionReference chatroomMessagesRef(String chatroomId) {
        return chatroomRef(chatroomId).collection("chats");
    }

    public static String getChatroomId(String userId1, String userId2) {
        // Deterministic ID regardless of sender/receiver order
        return (userId1.hashCode() < userId2.hashCode())
                ? userId1 + "_" + userId2
                : userId2 + "_" + userId1;
    }

    public static DocumentReference getOtherUserFromChatroom(List<String> userIds) {
        if (userIds.get(0).equals(currentUserId())) {
            return usersCollection().document(userIds.get(1));
        } else {
            return usersCollection().document(userIds.get(0));
        }
    }

    // ------------------------- 📸 STORAGE -------------------------
    public static StorageReference getCurrentProfilePicStorageRef() {
        return FirebaseStorage.getInstance()
                .getReference()
                .child("profile_pic")
                .child(currentUserId());
    }

    public static StorageReference getOtherProfilePicStorageRef(String otherUserId) {
        return FirebaseStorage.getInstance()
                .getReference()
                .child("profile_pic")
                .child(otherUserId);
    }

    // ------------------------- 🕓 UTILS -------------------------
    public static String timestampToString(Timestamp timestamp) {
        return new SimpleDateFormat("HH:mm").format(timestamp.toDate());
    }
}
