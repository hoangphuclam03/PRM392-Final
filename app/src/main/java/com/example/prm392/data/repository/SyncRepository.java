package com.example.prm392.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.local.UserDAO;
import com.example.prm392.models.ProjectEntity;
import com.example.prm392.models.ProjectMemberEntity;
import com.example.prm392.models.TaskEntity;
import com.example.prm392.models.UserEntity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

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
    }

    // =============================================================
    // 🔁 MASTER SYNC - GỌI TOÀN BỘ CÁC HÀM ĐỒNG BỘ
    // =============================================================
    public void syncAll() {
        executor.execute(() -> {
            try {
                syncProjectsToFirestore();
                syncMembersToFirestore();
                syncProjectsFromFirestore();
                syncMembersFromFirestore();
                syncTasksToFirestore();
                syncTasksFromFirestore();
                syncUsersFromFirestore(); // 🔥 NEW: pulls all users into local Room
            } catch (Exception e) {
                Log.e("SyncRepo", "❌ syncAll failed: " + e.getMessage(), e);
            }
        });
    }

    // =============================================================
    // 🔹 PROJECTS
    // =============================================================
    public void syncProjectsToFirestore() {
        List<ProjectEntity> pendingProjects = localDb.projectDAO().getPendingProjects();
        if (pendingProjects == null || pendingProjects.isEmpty()) return;

        Log.d("SyncRepo", "⬆ Uploading " + pendingProjects.size() + " projects → Firestore");

        for (ProjectEntity project : pendingProjects) {
            if (project.projectId == null || project.projectId.trim().isEmpty()) {
                project.projectId = UUID.randomUUID().toString();
                executor.execute(() -> localDb.projectDAO().insertOrUpdate(project));
                Log.w("SyncRepo", "Generated missing ID for " + project.projectName);
            }

            final String pid = project.projectId;

            remoteDb.collection("projects")
                    .document(pid)
                    .set(project, SetOptions.merge())
                    .addOnSuccessListener(a -> executor.execute(() -> {
                        localDb.projectDAO().markSynced(pid, System.currentTimeMillis());
                        Log.d("SyncRepo", "✅ Synced project " + project.projectName);
                    }))
                    .addOnFailureListener(e ->
                            Log.e("SyncRepo", "❌ Failed to upload project " + project.projectName, e)
                    );
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

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            UserEntity user = new UserEntity();
                            user.userId = doc.getId();
                            user.fullName = doc.getString("fullName");
                            user.email = doc.getString("email");
                            user.avatarUrl = doc.getString("avatarUrl");
                            user.password = ""; // never sync password from Firebase
                            Long lastLogin = doc.getLong("lastLogin");
                            user.lastLogin = lastLogin != null ? lastLogin : 0L;

                            userDAO.insertOrUpdate(user);
                        }

                        Log.d("SyncRepo", "✅ Synced " + snapshot.size() + " users from Firestore → Room");
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
