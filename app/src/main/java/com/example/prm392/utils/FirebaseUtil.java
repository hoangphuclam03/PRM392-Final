package com.example.prm392.utils;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified Firebase utility helper
 * Supports: Auth, Firestore (Users, Projects, Tasks, Teams, Chatrooms), and Storage.
 */
public class FirebaseUtil {

    // ------------------------- 🔐 AUTH + FIRESTORE CORE -------------------------
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();
    public static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static FirebaseAuth getAuth() { return auth; }
    public static FirebaseFirestore getDatabase() { return db; }

    // ------------------------- 👥 USERS -------------------------
    public static CollectionReference usersCollection() { return db.collection("Users"); }

    public static DocumentReference currentUserDetails() {
        String uid = currentUserId();
        return usersCollection().document(uid != null ? uid : "guest_user");
    }

    /** ✅ Trả về null khi chưa đăng nhập (rất quan trọng cho các check) */
    public static String currentUserId() {
        FirebaseUser u = auth.getCurrentUser();
        return (u != null) ? u.getUid() : null;
    }

    public static boolean isLoggedIn() { return auth.getCurrentUser() != null; }
    public static void logout() { auth.signOut(); }

    // ------------------------- 🗂️ PROJECTS -------------------------
    /** Giữ nguyên tên collection bạn đang dùng */

    public static CollectionReference projectsCollection() {
        return db.collection("projects"); // phải là chữ thường
    }
    public static DocumentReference projectRef(String projectId) {
        return projectsCollection().document(projectId);
    }
    public static CollectionReference projectMessagesRef(String projectId) {
        return projectRef(projectId).collection("messages");
    }
    
    /** (tuỳ chọn) Members subcollection của project */
    public static CollectionReference projectMembersRef(String projectId) {
        return projectRef(projectId).collection("members");
    }

    // ------------------------- ✅ TASKS -------------------------
    public static CollectionReference tasksCollection() { return db.collection("Tasks"); }
    public static DocumentReference taskRef(String taskId) { return tasksCollection().document(taskId); }

    // ------------------------- 🧑‍🤝‍🧑 TEAMS (legacy – vẫn giữ để không vỡ code cũ) -------------------------
    public static CollectionReference teamsCollection() { return db.collection("teams"); }
    public static DocumentReference teamRef(String teamId) { return teamsCollection().document(teamId); }
    public static CollectionReference teamMessagesRef(String teamId) { return teamRef(teamId).collection("messages"); }

    // ------------------------- 💬 CHATROOMS -------------------------
    public static CollectionReference allChatroomsCollection() { return db.collection("chatrooms"); }
    public static DocumentReference chatroomRef(String chatroomId) { return allChatroomsCollection().document(chatroomId); }
    public static CollectionReference chatroomMessagesRef(String chatroomId) { return chatroomRef(chatroomId).collection("chats"); }

    public static String getChatroomId(String userId1, String userId2) {
        return (userId1.hashCode() < userId2.hashCode())
                ? userId1 + "_" + userId2
                : userId2 + "_" + userId1;
    }

    public static DocumentReference getOtherUserFromChatroom(List<String> userIds) {
        String me = currentUserId();
        if (me != null && userIds.get(0).equals(me)) {
            return usersCollection().document(userIds.get(1));
        } else {
            return usersCollection().document(userIds.get(0));
        }
    }

    // ------------------------- 📸 STORAGE -------------------------
    public static StorageReference getCurrentProfilePicStorageRef() {
        String uid = currentUserId();
        return FirebaseStorage.getInstance().getReference()
                .child("profile_pic").child(uid != null ? uid : "guest_user");
    }

    public static StorageReference getOtherProfilePicStorageRef(String otherUserId) {
        return FirebaseStorage.getInstance().getReference()
                .child("profile_pic").child(otherUserId);
    }

    // ------------------------- 🕓 UTILS -------------------------
    public static String timestampToString(Timestamp timestamp) {
        return new SimpleDateFormat("HH:mm").format(timestamp.toDate());
    }

    // ------------------------- 🔔 NOTIFICATIONS -------------------------
    /** Legacy: gửi notif cho team */
    public static void sendTeamNotification(String teamId, String message) {
        String senderId = currentUserId();
        if (senderId == null) return;

        teamsCollection().document(teamId)
                .collection("members")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String memberId = doc.getString("userId");
                        if (memberId == null || memberId.equals(senderId)) continue;

                        String fcmToken = doc.getString("fcmToken");
                        if (fcmToken == null) continue;

                        sendNotificationToToken(fcmToken, message);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("FirebaseUtil", "Failed sending team notifications: " + e.getMessage()));
    }

    /** ✅ Gửi notif theo PROJECT */
    public static void sendProjectNotification(String projectId, String message) {
        String senderId = currentUserId();
        if (senderId == null) return;

        projectMembersRef(projectId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String memberId = doc.getString("userId");
                        if (memberId == null || memberId.equals(senderId)) continue;

                        String fcmToken = doc.getString("fcmToken");
                        if (fcmToken == null) continue;

                        sendNotificationToToken(fcmToken, message);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("FirebaseUtil", "Failed sending project notifications: " + e.getMessage()));
    }

    /** Placeholder: gọi Cloud Function / server của bạn trong production */
    private static void sendNotificationToToken(String token, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("to", token);
        payload.put("notification", new HashMap<String, String>() {{
            put("title", "New Message");
            put("body", message);
        }});
        Log.d("FirebaseUtil", "Notification to " + token + ": " + message);
    }

    // === Aliases cũ để không vỡ import (khuyến khích chuyển dần sang project*) ===
    public static CollectionReference getTeamCollection() { return teamsCollection(); }
    public static DocumentReference getTeamRef(String teamId) { return teamRef(teamId); }
    public static CollectionReference getTeamMessagesRef(String teamId) { return teamMessagesRef(teamId); }
}
