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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncRepository {

    private final Context context;
    private final AppDatabase localDb;
    private final FirebaseFirestore remoteDb;
    private final ExecutorService executor;

    public SyncRepository(Context context) {
        this.context = context;   // ✅ Fix missing context
        this.localDb = AppDatabase.getInstance(context);
        this.remoteDb = FirebaseFirestore.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
    }

    // =============================================================
    // 🔁 SYNC ALL
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
                syncUsersFromFirestore();
            } catch (Exception e) {
                Log.e("SyncRepo", "❌ syncAll failed: " + e.getMessage(), e);
            }
        });
    }

    // =============================================================
    // 🔹 PROJECTS
    // =============================================================
    public void syncProjectsToFirestore() {
        List<ProjectEntity> pending = localDb.projectDAO().getPendingProjects();
        if (pending == null || pending.isEmpty()) return;

        for (ProjectEntity p : pending) {
            if (p.projectId == null || p.projectId.trim().isEmpty()) {
                p.projectId = UUID.randomUUID().toString();
                localDb.projectDAO().insertOrUpdate(p);
            }

            remoteDb.collection("projects")
                    .document(p.projectId)
                    .set(p, SetOptions.merge())
                    .addOnSuccessListener(a -> executor.execute(() ->
                            localDb.projectDAO().markSynced(p.projectId, System.currentTimeMillis())
                    ))
                    .addOnFailureListener(e ->
                            Log.e("SyncRepo", "❌ Upload project failed", e)
                    );
        }
    }
    public void refreshProjectsFromFirestore() {
        remoteDb.collection("projects")
                .get()
                .addOnSuccessListener(snapshot -> executor.execute(() -> {
                    try {
                        // Xoá sạch local để nạp mới
                        localDb.projectDAO().clearAll();

                        if (snapshot == null || snapshot.isEmpty()) {
                            Log.d("SyncRepo", "Firestore 'projects' trống → đã clear Room");
                            return;
                        }

                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                            ProjectEntity p = doc.toObject(ProjectEntity.class);
                            if (p.projectId == null || p.projectId.trim().isEmpty()) {
                                p.projectId = doc.getId();
                            }
                            localDb.projectDAO().insertOrUpdate(p);
                        }
                        Log.d("SyncRepo", "Refreshed projects Firestore → Room OK");
                    } catch (Exception e) {
                        Log.e("SyncRepo", "Error refreshing projects", e);
                    }
                }))
                .addOnFailureListener(e -> Log.e("SyncRepo", "Firestore refresh failed", e));
    }

    // =============================================================
// 🗑 XOÁ PROJECT + TẤT CẢ MEMBERS (Firestore + Room)
// =============================================================
    public void deleteProjectAndMembers(String projectId) {
        Log.d("SyncRepo", "deleteProjectAndMembers → " + projectId);

        // 1) Xoá members trên Firestore
        remoteDb.collection("project_members")
                .whereEqualTo("projectId", projectId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    snapshot.getDocuments().forEach(d ->
                            d.getReference().delete()
                                    .addOnSuccessListener(v ->
                                            Log.d("SyncRepo", "Deleted member " + d.getId()))
                                    .addOnFailureListener(err ->
                                            Log.e("SyncRepo", "Delete member failed " + d.getId(), err))
                    );

                    // 2) Xoá project doc trên Firestore
                    remoteDb.collection("projects")
                            .document(projectId)
                            .delete()
                            .addOnSuccessListener(v -> {
                                Log.d("SyncRepo", "Deleted project doc " + projectId);

                                // 3) Xoá local trong Room
                                executor.execute(() -> {
                                    try {
                                        localDb.projectMemberDAO().deleteByProject(projectId);
                                        localDb.projectDAO().deleteById(projectId);
                                        Log.d("SyncRepo", "Deleted local project + members " + projectId);
                                    } catch (Exception e) {
                                        Log.e("SyncRepo", "Error deleting local data", e);
                                    }
                                });
                            })
                            .addOnFailureListener(err ->
                                    Log.e("SyncRepo", "Delete project doc failed " + projectId, err));
                })
                .addOnFailureListener(err ->
                        Log.e("SyncRepo", "Fetch members to delete failed for " + projectId, err));
    }
    public void syncProjectsFromFirestore() {
        remoteDb.collection("projects")
                .get()
                .addOnSuccessListener(snapshot -> executor.execute(() -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        ProjectEntity p = doc.toObject(ProjectEntity.class);
                        if (p.projectId == null) p.projectId = doc.getId();
                        localDb.projectDAO().insertOrUpdate(p);
                    }
                }))
                .addOnFailureListener(e ->
                        Log.e("SyncRepo", "❌ Fetch projects failed", e)
                );
    }

    // =============================================================
    // 🔹 MEMBERS
    // =============================================================
    public void syncMembersToFirestore() {
        List<ProjectMemberEntity> members = localDb.projectMemberDAO().getPendingMembers();
        if (members == null || members.isEmpty()) return;

        for (ProjectMemberEntity m : members) {
            if (m.memberId == null || m.memberId.trim().isEmpty()) {
                m.memberId = UUID.randomUUID().toString();
                localDb.projectMemberDAO().insertOrUpdate(m);
            }

            remoteDb.collection("project_members")
                    .document(m.memberId)
                    .set(m, SetOptions.merge())
                    .addOnSuccessListener(a -> executor.execute(() -> {
                        m.pendingSync = false;
                        m.updatedAt = System.currentTimeMillis();
                        localDb.projectMemberDAO().insertOrUpdate(m);
                    }))
                    .addOnFailureListener(e ->
                            Log.e("SyncRepo", "❌ Upload member failed", e)
                    );
        }
    }

    public void syncMembersFromFirestore() {
        remoteDb.collection("project_members")
                .get()
                .addOnSuccessListener(snapshot -> executor.execute(() -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        ProjectMemberEntity m = doc.toObject(ProjectMemberEntity.class);
                        if (m.memberId == null) m.memberId = doc.getId();
                        localDb.projectMemberDAO().insertOrUpdate(m);
                    }
                }))
                .addOnFailureListener(e ->
                        Log.e("SyncRepo", "❌ Fetch members failed", e)
                );
    }

    // =============================================================
    // 🔹 TASKS
    // =============================================================
    public void syncTasksToFirestore() {
        List<TaskEntity> tasks = localDb.taskDAO().getPendingSyncTasks();
        if (tasks == null || tasks.isEmpty()) return;

        for (TaskEntity t : tasks) {
            if (t.taskId == null || t.taskId.trim().isEmpty()) {
                t.taskId = UUID.randomUUID().toString();
                localDb.taskDAO().insertOrUpdate(t);
            }

            remoteDb.collection("tasks")
                    .document(t.taskId)
                    .set(t, SetOptions.merge())
                    .addOnSuccessListener(a -> executor.execute(() ->
                            localDb.taskDAO().markSynced(t.taskId, System.currentTimeMillis())
                    ))
                    .addOnFailureListener(e ->
                            Log.e("SyncRepo", "❌ Upload task failed", e)
                    );
        }
    }

    public void syncTasksFromFirestore() {
        remoteDb.collection("tasks")
                .get()
                .addOnSuccessListener(snapshot -> executor.execute(() -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        TaskEntity t = doc.toObject(TaskEntity.class);
                        if (t.taskId == null) t.taskId = doc.getId();
                        localDb.taskDAO().insertOrUpdate(t);
                    }
                }))
                .addOnFailureListener(e ->
                        Log.e("SyncRepo", "❌ Fetch tasks failed", e)
                );
    }

    // =============================================================
    // 🔹 USERS
    // =============================================================
    public void syncUsersFromFirestore() {
        remoteDb.collection("Users")
                .get()
                .addOnSuccessListener(snapshot -> executor.execute(() -> {

                    UserDAO userDAO = localDb.userDAO();
                    List<UserEntity> localUsers = userDAO.getAll();
                    List<String> firestoreIds = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        firestoreIds.add(doc.getId());

                        UserEntity u = new UserEntity();
                        u.userId = doc.getId();
                        u.fullName = doc.getString("fullName");
                        u.email = doc.getString("email");
                        u.avatarUrl = doc.getString("avatarUrl");
                        u.password = "";

                        Long lastLogin = doc.getLong("lastLogin");
                        u.lastLogin = lastLogin == null ? 0L : lastLogin;

                        userDAO.insertOrUpdate(u);
                    }

                    for (UserEntity u : localUsers) {
                        if (!firestoreIds.contains(u.userId)) {
                            userDAO.deleteUser(u.userId);
                        }
                    }

                }))
                .addOnFailureListener(e ->
                        Log.e("SyncRepo", "❌ Fetch users failed", e)
                );
    }
}
