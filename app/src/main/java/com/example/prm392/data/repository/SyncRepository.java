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

public class SyncRepository {
    private final AppDatabase localDb;
    private final FirebaseFirestore remoteDb;

    public SyncRepository(Context context) {
        this.localDb = AppDatabase.getInstance(context);
        this.remoteDb = FirebaseFirestore.getInstance();
    }

    // ------------------------- PROJECTS SYNC -------------------------
    public void syncProjectsFromFirestore() {
        remoteDb.collection("projects")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        ProjectEntity project = doc.toObject(ProjectEntity.class);
                        localDb.projectDAO().insertOrUpdate(project);
                    }
                    Log.d("SyncRepo", "✅ Synced projects from Firestore to Room");
                })
                .addOnFailureListener(e -> Log.e("SyncRepo", "❌ Failed to fetch projects", e));
    }

    public void syncProjectsToFirestore() {
        new Thread(() -> {
            List<ProjectEntity> pending = localDb.projectDAO().getAll();
            for (ProjectEntity p : pending) {
                remoteDb.collection("projects")
                        .document(String.valueOf(p.projectId))
                        .set(p, SetOptions.merge())
                        .addOnSuccessListener(a ->
                                Log.d("SyncRepo", "Uploaded project " + p.projectName)
                        )
                        .addOnFailureListener(e ->
                                Log.e("SyncRepo", "Failed to upload project", e)
                        );
            }
        }).start();
    }

    // ------------------------- TASKS SYNC -------------------------
    public void syncTasksFromFirestore() {
        remoteDb.collection("tasks")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (QueryDocumentSnapshot doc : snapshot) {
                        TaskEntity task = doc.toObject(TaskEntity.class);
                        localDb.taskDAO().insertOrUpdate(task);
                    }
                    Log.d("SyncRepo", "✅ Synced tasks from Firestore to Room");
                })
                .addOnFailureListener(e -> Log.e("SyncRepo", "❌ Failed to fetch tasks", e));
    }

    public void syncTasksToFirestore() {
        new Thread(() -> {
            List<TaskEntity> pending = localDb.taskDAO().getPendingSyncTasks();
            for (TaskEntity t : pending) {
                remoteDb.collection("tasks")
                        .document(String.valueOf(t.taskId))
                        .set(t, SetOptions.merge())
                        .addOnSuccessListener(a ->
                                Log.d("SyncRepo", "Uploaded task " + t.title)
                        )
                        .addOnFailureListener(e ->
                                Log.e("SyncRepo", "Failed to upload task", e)
                        );
            }
        }).start();
    }
}
