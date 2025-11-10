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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

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

            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) {
                Log.e("SyncRepo", "🚫 No authenticated Firebase user. Firestore will not sync.");
                return;
            }
            String uid = auth.getCurrentUser().getUid();

            WriteBatch batch = remoteDb.batch();
            int uploadCount = 0;
            final int finalUploadCount = uploadCount;
            for (ProjectEntity project : pendingProjects) {
                // --- Skip unchanged projects ---
                if (project.lastSyncedAt >= project.updatedAt) {
                    Log.d("SyncRepo", "⏭ Skipping unchanged project: " + project.projectName);
                    continue;
                }

                // --- Validate ---
                if (project.projectId == null || project.projectId.trim().isEmpty())
                    project.projectId = UUID.randomUUID().toString();
                if (project.createdBy == null || project.createdBy.trim().isEmpty())
                    project.createdBy = uid;
                if (project.projectName == null || project.projectName.trim().isEmpty())
                    project.projectName = "Untitled Project";
                if (project.description == null)
                    project.description = "";

                DocumentReference ref = remoteDb.collection("projects").document(project.projectId);
                batch.set(ref, project, SetOptions.merge());
                uploadCount++;
            }

            if (uploadCount == 0) {
                Log.d("SyncRepo", "✅ All projects already up to date. No writes needed.");
                return;
            }

            Log.d("SyncRepo", "🚀 Executing Firestore batch upload (" + uploadCount + " projects)...");

            long now = System.currentTimeMillis();
            batch.commit()
                    .addOnSuccessListener(a -> executor.execute(() -> {
                        for (ProjectEntity project : pendingProjects) {
                            if (project.lastSyncedAt < project.updatedAt) {
                                project.pendingSync = false;
                                project.lastSyncedAt = now;
                                localDb.projectDAO().insertOrUpdate(project);
                            }
                        }
                        Log.d("SyncRepo", "🟩 Firestore batch SUCCESS (" + finalUploadCount + " projects)");
                    }))
                    .addOnFailureListener(e -> Log.e("SyncRepo", "❌ Firestore batch FAILED: " + e.getMessage(), e));

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
        try {
            List<ProjectMemberEntity> pendingMembers = localDb.projectMemberDAO().getPendingMembers();
            int pendingCount = (pendingMembers == null ? 0 : pendingMembers.size());
            Log.d("SyncRepo", "🧩 Pending members count = " + pendingCount);

            if (pendingMembers == null || pendingMembers.isEmpty()) {
                Log.d("SyncRepo", "⚠️ No pending members to upload.");
                return;
            }

            WriteBatch batch = remoteDb.batch();
            int uploadCount = 0;
            final int finalUploadCount = uploadCount;
            for (ProjectMemberEntity member : pendingMembers) {
                if (member.updatedAt <= member.lastSyncedAt) {
                    Log.d("SyncRepo", "⏭ Skipping unchanged member: " + member.fullName);
                    continue;
                }

                if (member.memberId == null || member.memberId.trim().isEmpty())
                    member.memberId = UUID.randomUUID().toString();

                DocumentReference ref = remoteDb.collection("project_members").document(member.memberId);
                batch.set(ref, member, SetOptions.merge());
                uploadCount++;
            }

            if (uploadCount == 0) {
                Log.d("SyncRepo", "✅ All members already up to date. No writes needed.");
                return;
            }

            Log.d("SyncRepo", "🚀 Executing Firestore batch upload (" + uploadCount + " members)...");

            long now = System.currentTimeMillis();
            batch.commit()
                    .addOnSuccessListener(a -> executor.execute(() -> {
                        for (ProjectMemberEntity member : pendingMembers) {
                            if (member.lastSyncedAt < member.updatedAt) {
                                member.pendingSync = false;
                                member.lastSyncedAt = now;
                                localDb.projectMemberDAO().insertOrUpdate(member);
                            }
                        }
                        Log.d("SyncRepo", "🟩 Firestore batch SUCCESS (" + finalUploadCount + " members)");
                    }))
                    .addOnFailureListener(e -> Log.e("SyncRepo", "❌ Firestore member batch FAILED: " + e.getMessage(), e));

        } catch (Exception ex) {
            Log.e("SyncRepo", "💥 syncMembersToFirestore crashed: " + ex.getMessage(), ex);
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
        try {
            List<TaskEntity> pendingTasks = localDb.taskDAO().getPendingSyncTasks();
            int pendingCount = (pendingTasks == null ? 0 : pendingTasks.size());
            Log.d("SyncRepo", "🧩 Pending tasks count = " + pendingCount);

            if (pendingTasks == null || pendingTasks.isEmpty()) {
                Log.d("SyncRepo", "⚠️ No pending tasks to upload.");
                return;
            }

            WriteBatch batch = remoteDb.batch();
            int uploadCount = 0;
            final int finalUploadCount = uploadCount;
            for (TaskEntity task : pendingTasks) {
                if (task.lastSyncedAt >= task.updatedAt) {
                    Log.d("SyncRepo", "⏭ Skipping unchanged task: " + task.title);
                    continue;
                }

                if (task.taskId == null || task.taskId.trim().isEmpty())
                    task.taskId = UUID.randomUUID().toString();
                if (task.title == null || task.title.trim().isEmpty())
                    task.title = "Untitled Task";

                DocumentReference ref = remoteDb.collection("tasks").document(task.taskId);
                batch.set(ref, task, SetOptions.merge());
                uploadCount++;
            }

            if (uploadCount == 0) {
                Log.d("SyncRepo", "✅ All tasks already up to date. No writes needed.");
                return;
            }

            Log.d("SyncRepo", "🚀 Executing Firestore batch upload (" + uploadCount + " tasks)...");

            long now = System.currentTimeMillis();
            batch.commit()
                    .addOnSuccessListener(a -> executor.execute(() -> {
                        for (TaskEntity task : pendingTasks) {
                            if (task.lastSyncedAt < task.updatedAt) {
                                task.pendingSync = false;
                                task.lastSyncedAt = now;
                                localDb.taskDAO().insertOrUpdate(task);
                            }
                        }
                        Log.d("SyncRepo", "🟩 Firestore batch SUCCESS (" + finalUploadCount + " tasks)");
                    }))
                    .addOnFailureListener(e -> Log.e("SyncRepo", "❌ Firestore task batch FAILED: " + e.getMessage(), e));

        } catch (Exception ex) {
            Log.e("SyncRepo", "💥 syncTasksToFirestore crashed: " + ex.getMessage(), ex);
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
