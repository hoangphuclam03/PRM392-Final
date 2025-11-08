package com.example.prm392.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.models.ProjectEntity;
import com.example.prm392.models.TaskEntity;
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
    private final ExecutorService executor; // ensures DB ops always off the main thread

    public SyncRepository(Context context) {
        this.localDb = AppDatabase.getInstance(context);
        this.remoteDb = FirebaseFirestore.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
    }

    // 🔁 Master method - triggers all sync logic
    public void syncAll() {
        executor.execute(() -> {
            try {
                syncProjectsToFirestore();
                syncProjectsFromFirestore();
                syncTasksToFirestore();
                syncTasksFromFirestore();
            } catch (Exception e) {
                Log.e("SyncRepo", "syncAll failed: " + e.getMessage(), e);
            }
        });
    }

    // ------------------------- PROJECTS -------------------------

    public void syncProjectsToFirestore() {
        List<ProjectEntity> pendingProjects = localDb.projectDAO().getPendingProjects();
        if (pendingProjects == null || pendingProjects.isEmpty()) return;

        Log.d("SyncRepo", "Uploading " + pendingProjects.size() + " projects → Firestore");

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
                            if (project.projectId == null) {
                                project.projectId = doc.getId();
                            }
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

    // ------------------------- TASKS -------------------------

    public void syncTasksToFirestore() {
        List<TaskEntity> pendingTasks = localDb.taskDAO().getPendingSyncTasks();
        if (pendingTasks == null || pendingTasks.isEmpty()) return;

        Log.d("SyncRepo", "Uploading " + pendingTasks.size() + " tasks → Firestore");

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
}
