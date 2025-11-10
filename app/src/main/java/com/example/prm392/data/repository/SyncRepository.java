package com.example.prm392.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.local.UserDAO;
import com.example.prm392.models.ProjectEntity;
import com.example.prm392.models.ProjectMemberEntity;
import com.example.prm392.models.TaskEntity;
import com.example.prm392.models.UserEntity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncRepository {

    private final AppDatabase localDb;
    private final FirebaseFirestore remoteDb;
    private final ExecutorService executor;

    public SyncRepository(Context context) {
        this.localDb = AppDatabase.getInstance(context);
        this.remoteDb = FirebaseFirestore.getInstance();
        this.executor = Executors.newSingleThreadExecutor();

        // ✅ Firestore settings can only be set BEFORE any Firestore usage.
        try {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(false) // or true if you want caching later
                    .build();
            remoteDb.setFirestoreSettings(settings);
            Log.d("SyncRepo", "⚙️ Firestore settings applied successfully.");
        } catch (IllegalStateException e) {
            Log.w("SyncRepo", "⚠️ Firestore already started; settings unchanged: " + e.getMessage());
        }

        // ✅ Make sure Firestore network is enabled once
        remoteDb.enableNetwork()
                .addOnSuccessListener(a -> Log.d("SyncRepo", "🌐 Firestore network enabled"))
                .addOnFailureListener(e -> Log.e("SyncRepo", "❌ Failed to enable Firestore network", e));
    }
    // =============================================================
    // 🔁 MASTER SYNC - GỌI TOÀN BỘ CÁC HÀM ĐỒNG BỘ
    // =============================================================
    public void syncAll() {
        executor.execute(() -> {
            try {
                Log.d("SyncRepo", "🚀 Starting staged sync...");

                syncProjectsToFirestore();
                Thread.sleep(400);
                syncMembersToFirestore();
                Thread.sleep(400);
                syncTasksToFirestore();
                Thread.sleep(400);

                // small wait for Firestore commit
                try {
                    Thread.sleep(800);
                } catch (InterruptedException ignored) {
                }

                syncProjectsFromFirestore();
                Thread.sleep(400);
                syncMembersFromFirestore();
                Thread.sleep(400);
                syncTasksFromFirestore();
                Thread.sleep(400);
                syncUsersFromFirestore();
                Thread.sleep(400);

                Log.d("SyncRepo", "✅ Staged sync completed successfully.");
            } catch (Exception e) {
                Log.e("SyncRepo", "❌ syncAll failed: " + e.getMessage(), e);
            }
        });
    }


    // =============================================================
    // 🔹 PROJECTS
    // =============================================================
    public void syncProjectsToFirestore() {
        try {
            List<ProjectEntity> pendingProjects = localDb.projectDAO().getPendingProjects();
            int pendingCount = (pendingProjects == null ? 0 : pendingProjects.size());
            Log.d("SyncRepo", "🧩 Pending projects count = " + pendingCount);

            if (pendingProjects == null || pendingProjects.isEmpty()) {
                Log.d("SyncRepo", "⚠️ No pending projects to upload. Nothing to sync.");
                return;
            }

            // --- Check FirebaseAuth user ---
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) {
                Log.e("SyncRepo", "🚫 No authenticated Firebase user. Firestore will not sync.");
                return;
            }
            Log.d("SyncRepo", "👤 Current Firebase UID: " + auth.getCurrentUser().getUid());

            for (ProjectEntity project : pendingProjects) {
                // --- Validate ---
                if (project.projectId == null || project.projectId.trim().isEmpty()) {
                    project.projectId = UUID.randomUUID().toString();
                    Log.w("SyncRepo", "🆕 Generated missing Firestore ID for: " + project.projectName);
                }
                if (project.createdBy == null || project.createdBy.trim().isEmpty()) {
                    project.createdBy = auth.getCurrentUser().getUid();
                    Log.w("SyncRepo", "⚠️ Missing createdBy → set to UID: " + project.createdBy);
                }
                if (project.projectName == null || project.projectName.trim().isEmpty()) {
                    project.projectName = "Untitled Project";
                }
                if (project.description == null) project.description = "";

                final String pid = project.projectId;
                Log.d("SyncRepo", "⬆ Starting upload for project: " + project.projectName + " (" + pid + ")");

                // --- Quick connectivity test ---
                remoteDb.collection("projects").limit(1).get()
                        .addOnCompleteListener(test -> {
                            if (test.isSuccessful()) {
                                Log.d("SyncRepo", "✅ Firestore connectivity OK before upload.");
                            } else {
                                Log.w("SyncRepo", "⚠️ Firestore connectivity check failed (might be offline).");
                            }
                        });

                // --- Actual upload ---
                remoteDb.collection("projects")
                        .document(pid)
                        .set(project, SetOptions.merge())
                        .addOnCompleteListener(task -> {
                            Log.d("SyncRepo", "📩 Firestore onComplete → " + project.projectName +
                                    ", success=" + task.isSuccessful());
                            if (!task.isSuccessful() && task.getException() != null) {
                                Log.e("SyncRepo", "❌ Firestore task exception: " +
                                        task.getException().getMessage(), task.getException());
                            }
                        })
                        .addOnSuccessListener(a -> {
                            Log.d("SyncRepo", "🟩 Firestore write SUCCESS for: " + project.projectName);
                            executor.execute(() -> {
                                localDb.projectDAO().markSynced(pid, System.currentTimeMillis());
                                Log.d("SyncRepo", "✅ Local DB updated (marked synced) → " + project.projectName);
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e("SyncRepo", "❌ Firestore write FAILED for: " + project.projectName +
                                    " | Reason: " + e.getMessage(), e);
                            project.pendingSync = true;
                            executor.execute(() -> {
                                localDb.projectDAO().insertOrUpdate(project);
                                Log.w("SyncRepo", "🔁 Requeued project for retry: " + project.projectName);
                            });
                        });

                // --- Offline queue tracking ---
                remoteDb.collection("projects").document(pid)
                        .addSnapshotListener((snapshot, error) -> {
                            if (error != null) {
                                Log.w("SyncRepo", "⚠️ Snapshot listener error for " +
                                        project.projectName + ": " + error.getMessage());
                                return;
                            }
                            if (snapshot != null && snapshot.getMetadata().hasPendingWrites()) {
                                Log.d("SyncRepo", "🕓 Queued (offline): " + project.projectName +
                                        " → waiting for network commit");
                            } else if (snapshot != null) {
                                Log.d("SyncRepo", "💾 Synced (server confirmed): " + project.projectName);
                            }
                        });
            }

        } catch (Exception ex) {
            Log.e("SyncRepo", "💥 syncProjectsToFirestore crashed: " + ex.getMessage(), ex);
        }
    }
    public void syncProjectsFromFirestore() {
        remoteDb.collection("projects")
                .get()
                .addOnSuccessListener(snapshot -> executor.execute(() -> {
                    try {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            ProjectEntity project = doc.toObject(ProjectEntity.class);
                            if (project.projectId == null)
                                project.projectId = doc.getId();
                            localDb.projectDAO().insertOrUpdate(project);
                        }
                        Log.d("SyncRepo", "✅ Pulled projects from Firestore → Room");
                    } catch (Exception e) {
                        Log.e("SyncRepo", "❌ Error inserting Firestore projects", e);
                    }
                }))
                .addOnFailureListener(e ->
                        Log.e("SyncRepo", "❌ Fetch Firestore projects failed", e)
                );
    }

    // =============================================================
    // 🔹 PROJECT MEMBERS
    // =============================================================
    public void syncMembersToFirestore() {
        List<ProjectMemberEntity> pendingMembers = localDb.projectMemberDAO().getPendingMembers();
        if (pendingMembers == null || pendingMembers.isEmpty()) return;

        Log.d("SyncRepo", "⬆ Uploading " + pendingMembers.size() + " members → Firestore");

        for (ProjectMemberEntity member : pendingMembers) {
            if (member.memberId == null || member.memberId.trim().isEmpty()) {
                member.memberId = UUID.randomUUID().toString();
                executor.execute(() -> localDb.projectMemberDAO().insertOrUpdate(member));
            }

            final String mid = member.memberId;

            remoteDb.collection("project_members")
                    .document(mid)
                    .set(member, SetOptions.merge())
                    .addOnSuccessListener(a -> executor.execute(() -> {
                        member.pendingSync = false;
                        member.updatedAt = System.currentTimeMillis();
                        localDb.projectMemberDAO().insertOrUpdate(member);
                        Log.d("SyncRepo", "✅ Synced member " + member.fullName + " (" + member.role + ")");
                    }))
                    .addOnFailureListener(e ->
                            Log.e("SyncRepo", "❌ Failed to upload member " + member.fullName, e)
                    );
        }
    }

    public void syncMembersFromFirestore() {
        remoteDb.collection("project_members")
                .get()
                .addOnSuccessListener(snapshot -> executor.execute(() -> {
                    try {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            ProjectMemberEntity member = doc.toObject(ProjectMemberEntity.class);
                            if (member.memberId == null)
                                member.memberId = doc.getId();
                            localDb.projectMemberDAO().insertOrUpdate(member);
                        }
                        Log.d("SyncRepo", "✅ Pulled members from Firestore → Room");
                    } catch (Exception e) {
                        Log.e("SyncRepo", "❌ Error inserting Firestore members", e);
                    }
                }))
                .addOnFailureListener(e ->
                        Log.e("SyncRepo", "❌ Fetch Firestore members failed", e)
                );
    }

    // =============================================================
    // 🔹 TASKS
    // =============================================================
    public void syncTasksToFirestore() {
        List<TaskEntity> pendingTasks = localDb.taskDAO().getPendingSyncTasks();
        if (pendingTasks == null || pendingTasks.isEmpty()) return;

        Log.d("SyncRepo", "⬆ Uploading " + pendingTasks.size() + " tasks → Firestore");

        for (TaskEntity task : pendingTasks) {
            if (task.taskId == null || task.taskId.trim().isEmpty()) {
                task.taskId = UUID.randomUUID().toString();
                executor.execute(() -> localDb.taskDAO().insertOrUpdate(task));
            }

            final String tid = task.taskId;

            remoteDb.collection("tasks")
                    .document(tid)
                    .set(task, SetOptions.merge())
                    .addOnSuccessListener(a -> executor.execute(() -> {
                        localDb.taskDAO().markSynced(tid, System.currentTimeMillis());
                        Log.d("SyncRepo", "✅ Synced task " + task.title);
                    }))
                    .addOnFailureListener(e ->
                            Log.e("SyncRepo", "❌ Failed to upload task " + task.title, e)
                    );
        }
    }

    public void syncTasksFromFirestore() {
        remoteDb.collection("tasks")
                .get()
                .addOnSuccessListener(snapshot -> executor.execute(() -> {
                    try {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            TaskEntity task = doc.toObject(TaskEntity.class);
                            if (task.taskId == null)
                                task.taskId = doc.getId();
                            localDb.taskDAO().insertOrUpdate(task);
                        }
                        Log.d("SyncRepo", "✅ Pulled tasks from Firestore → Room");
                    } catch (Exception e) {
                        Log.e("SyncRepo", "❌ Error inserting Firestore tasks", e);
                    }
                }))
                .addOnFailureListener(e ->
                        Log.e("SyncRepo", "❌ Fetch Firestore tasks failed", e)
                );
    }

    // =============================================================
    // 🔹 USERS (NEW)
    // =============================================================
    public void syncUsersFromFirestore() {
        remoteDb.collection("Users")
                .get()
                .addOnSuccessListener(snapshot -> executor.execute(() -> {
                    try {
                        UserDAO userDAO = localDb.userDAO();

                        // 1️⃣ Get all current local users
                        List<UserEntity> localUsers = userDAO.getAll();

                        // 2️⃣ Build list of Firestore userIds
                        List<String> firestoreIds = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            firestoreIds.add(doc.getId());

                            // Convert Firestore doc → UserEntity
                            UserEntity user = new UserEntity();
                            user.userId = doc.getId();
                            user.fullName = doc.getString("fullName");
                            user.email = doc.getString("email");
                            user.avatarUrl = doc.getString("avatarUrl");
                            user.password = ""; // never sync password
                            Long lastLogin = doc.getLong("lastLogin");
                            user.lastLogin = lastLogin != null ? lastLogin : 0L;

                            userDAO.insertOrUpdate(user);
                        }

                        // 3️⃣ Delete local users not found in Firestore
                        for (UserEntity localUser : localUsers) {
                            if (!firestoreIds.contains(localUser.userId)) {
                                userDAO.deleteUser(localUser.userId);
                                Log.d("SyncRepo", "🗑 Deleted local user not in Firestore: " + localUser.userId);
                            }
                        }

                        Log.d("SyncRepo", "✅ Synced " + snapshot.size() + " users from Firestore → Room (with cleanup)");
                    } catch (Exception e) {
                        Log.e("SyncRepo", "❌ Error syncing users from Firestore", e);
                    }
                }))
                .addOnFailureListener(e ->
                        Log.e("SyncRepo", "❌ Fetch Firestore users failed", e)
                );
    }


    // =============================================================
    // 🔄 HARD REFRESH — luôn kéo dữ liệu mới nhất từ Firestore về Room
    // =============================================================
    public void refreshProjectsFromFirestore() {
        remoteDb.collection("projects")
                .get()
                .addOnSuccessListener(snapshot -> executor.execute(() -> {
                    try {
                        localDb.projectDAO().clearAll();

                        if (snapshot == null || snapshot.isEmpty()) {
                            Log.d("SyncRepo", "⚠️ Firestore empty, cleared local Room");
                            return;
                        }

                        for (QueryDocumentSnapshot doc : snapshot) {
                            ProjectEntity project = doc.toObject(ProjectEntity.class);
                            if (project.projectId == null)
                                project.projectId = doc.getId();
                            localDb.projectDAO().insertOrUpdate(project);
                        }
                        Log.d("SyncRepo", "✅ Refreshed projects from Firestore → Room");
                    } catch (Exception e) {
                        Log.e("SyncRepo", "❌ Error refreshing Firestore projects", e);
                    }
                }))
                .addOnFailureListener(e ->
                        Log.e("SyncRepo", "❌ Firestore refresh failed", e)
                );
    }

    // =============================================================
    // 🗑 DELETE PROJECT + ALL MEMBERS
    // =============================================================
    public void deleteProjectAndMembers(String projectId) {
        Log.d("SyncRepo", "🧩 deleteProjectAndMembers CALLED with projectId = " + projectId);

        remoteDb.collection("project_members")
                .whereEqualTo("projectId", projectId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        doc.getReference().delete();
                        Log.d("SyncRepo", "🗑 Deleted member: " + doc.getId());
                    }
                    Log.d("SyncRepo", "✅ Deleted all members for project " + projectId);

                    remoteDb.collection("projects")
                            .document(projectId)
                            .delete()
                            .addOnSuccessListener(a -> {
                                Log.d("SyncRepo", "✅ Deleted project doc " + projectId);

                                executor.execute(() -> {
                                    try {
                                        localDb.projectDAO().deleteById(projectId);
                                        localDb.projectMemberDAO().deleteByProject(projectId);
                                        Log.d("SyncRepo", "✅ Deleted local project + members " + projectId);
                                    } catch (Exception e) {
                                        Log.e("SyncRepo", "❌ Error deleting local data", e);
                                    }
                                });
                            })
                            .addOnFailureListener(e ->
                                    Log.e("SyncRepo", "❌ Failed delete project doc " + projectId, e)
                            );

                })
                .addOnFailureListener(e ->
                        Log.e("SyncRepo", "❌ Failed delete project members for " + projectId, e)
                );
    }

    public FirebaseFirestore getRemoteDb() {
        return remoteDb;
    }
}
