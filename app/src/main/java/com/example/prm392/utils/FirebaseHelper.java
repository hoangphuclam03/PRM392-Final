package com.example.prm392.utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import models.TaskAssignees;
import models.Tasks;

/**
 * Firebase Helper Class
 * Giúp đơn giản hóa các thao tác với Firebase
 */
public class FirebaseHelper {

    private static FirebaseHelper instance;
    private DatabaseReference dbRef;

    private FirebaseHelper() {
        dbRef = FirebaseDatabase.getInstance().getReference();
    }

    public static FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    public DatabaseReference getReference() {
        return dbRef;
    }

    // ============= TASKS =============

    public DatabaseReference getTasksRef() {
        return dbRef.child("tasks");
    }

    public DatabaseReference getTaskRef(int taskId) {
        return dbRef.child("tasks").child(String.valueOf(taskId));
    }

    public void createTask(Tasks task, OnSuccessListener listener) {
        getTaskRef(task.getTaskId())
                .setValue(task)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    public void updateTaskStatus(int taskId, String newStatus, OnSuccessListener listener) {
        getTaskRef(taskId)
                .child("status")
                .setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    public void deleteTask(int taskId, OnSuccessListener listener) {
        getTaskRef(taskId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    // ============= TASK ASSIGNEES =============

    public DatabaseReference getTaskAssigneesRef() {
        return dbRef.child("task_assignees");
    }

    public void assignUserToTask(TaskAssignees assignee, OnSuccessListener listener) {
        getTaskAssigneesRef()
                .child(String.valueOf(assignee.getId()))
                .setValue(assignee)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    // ============= PROJECTS =============

    public DatabaseReference getProjectsRef() {
        return dbRef.child("projects");
    }

    public DatabaseReference getProjectRef(int projectId) {
        return dbRef.child("projects").child(String.valueOf(projectId));
    }

    // ============= USERS =============

    public DatabaseReference getUsersRef() {
        return dbRef.child("users");
    }

    public DatabaseReference getUserRef(int userId) {
        return dbRef.child("users").child(String.valueOf(userId));
    }

    // ============= CALLBACKS =============

    public interface OnSuccessListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnDataLoadedListener<T> {
        void onDataLoaded(T data);
        void onError(String error);
    }
}