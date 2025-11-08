package com.example.prm392.utils;

import android.content.Context;
import android.os.AsyncTask;

import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.models.ChatEntity;
import com.example.prm392.models.ProjectEntity;
import com.example.prm392.models.ProjectMemberEntity;
import com.example.prm392.models.TaskEntity;
import com.example.prm392.models.UserEntity;

import java.util.List;

/**
 * Offline Replacement for FirebaseHelper using Room.
 * Keeps the same public API (method names, callbacks),
 * but all actions are done locally using DAOs.
 */
public class FirebaseHelper {

    private static FirebaseHelper instance;
    private final AppDatabase db;

    private FirebaseHelper(Context context) {
        db = AppDatabase.getInstance(context.getApplicationContext());
    }

    public static synchronized FirebaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseHelper(context);
        }
        return instance;
    }

    // =====================================================================
    // USERS
    // =====================================================================

    public void createUser(UserEntity user, OnSuccessListener listener) {
        AsyncTask.execute(() -> {
            try {
                db.userDAO().insertOrUpdate(user);
                if (listener != null) listener.onSuccess();
            } catch (Exception e) {
                if (listener != null) listener.onFailure(e.getMessage());
            }
        });
    }

    public void getUserByEmail(String email, OnDataLoadedListener<UserEntity> listener) {
        AsyncTask.execute(() -> {
            try {
                UserEntity user = db.userDAO().getUserByEmail(email);
                if (listener != null) listener.onDataLoaded(user);
            } catch (Exception e) {
                if (listener != null) listener.onError(e.getMessage());
            }
        });
    }

    public void getAllUsers(OnDataLoadedListener<List<UserEntity>> listener) {
        AsyncTask.execute(() -> {
            try {
                List<UserEntity> users = db.userDAO().getAllUsers();
                if (listener != null) listener.onDataLoaded(users);
            } catch (Exception e) {
                if (listener != null) listener.onError(e.getMessage());
            }
        });
    }

    // =====================================================================
    // PROJECTS
    // =====================================================================

    public void createProject(ProjectEntity project, OnSuccessListener listener) {
        AsyncTask.execute(() -> {
            try {
                db.projectDAO().insertOrUpdate(project);
                if (listener != null) listener.onSuccess();
            } catch (Exception e) {
                if (listener != null) listener.onFailure(e.getMessage());
            }
        });
    }

    public void deleteProject(String projectId, OnSuccessListener listener) {
        AsyncTask.execute(() -> {
            try {
                ProjectEntity p = db.projectDAO().getProjectById(projectId);
                if (p != null) db.projectDAO().delete(p);
                if (listener != null) listener.onSuccess();
            } catch (Exception e) {
                if (listener != null) listener.onFailure(e.getMessage());
            }
        });
    }

    public void getAllProjects(OnDataLoadedListener<List<ProjectEntity>> listener) {
        AsyncTask.execute(() -> {
            try {
                List<ProjectEntity> list = db.projectDAO().getAllProjects();
                if (listener != null) listener.onDataLoaded(list);
            } catch (Exception e) {
                if (listener != null) listener.onError(e.getMessage());
            }
        });
    }

    // =====================================================================
    // PROJECT MEMBERS
    // =====================================================================

    public void addProjectMember(ProjectMemberEntity member, OnSuccessListener listener) {
        AsyncTask.execute(() -> {
            try {
                db.projectMemberDAO().insert(member);
                if (listener != null) listener.onSuccess();
            } catch (Exception e) {
                if (listener != null) listener.onFailure(e.getMessage());
            }
        });
    }

    public void getMembersByProject(String projectId, OnDataLoadedListener<List<ProjectMemberEntity>> listener) {
        AsyncTask.execute(() -> {
            try {
                List<ProjectMemberEntity> members = db.projectMemberDAO().getMembersByProject(projectId);
                if (listener != null) listener.onDataLoaded(members);
            } catch (Exception e) {
                if (listener != null) listener.onError(e.getMessage());
            }
        });
    }

    // =====================================================================
    // TASKS
    // =====================================================================

    public void createTask(TaskEntity task, OnSuccessListener listener) {
        AsyncTask.execute(() -> {
            try {
                db.taskDAO().insertOrUpdate(task);
                if (listener != null) listener.onSuccess();
            } catch (Exception e) {
                if (listener != null) listener.onFailure(e.getMessage());
            }
        });
    }

    public void updateTaskStatus(String taskId, String status, OnSuccessListener listener) {
        AsyncTask.execute(() -> {
            try {
                db.taskDAO().updateTaskStatus(taskId, status);
                if (listener != null) listener.onSuccess();
            } catch (Exception e) {
                if (listener != null) listener.onFailure(e.getMessage());
            }
        });
    }

    public void getTasksByProject(String projectId, OnDataLoadedListener<List<TaskEntity>> listener) {
        AsyncTask.execute(() -> {
            try {
                List<TaskEntity> list = db.taskDAO().getTasksByProject(projectId);
                if (listener != null) listener.onDataLoaded(list);
            } catch (Exception e) {
                if (listener != null) listener.onError(e.getMessage());
            }
        });
    }

    // =====================================================================
    // CHAT
    // =====================================================================

    public void saveChat(ChatEntity chat, OnSuccessListener listener) {
        AsyncTask.execute(() -> {
            try {
                db.chatDAO().insert(chat);
                if (listener != null) listener.onSuccess();
            } catch (Exception e) {
                if (listener != null) listener.onFailure(e.getMessage());
            }
        });
    }

    public void getAllChats(OnDataLoadedListener<List<ChatEntity>> listener) {
        AsyncTask.execute(() -> {
            try {
                List<ChatEntity> chats = db.chatDAO().getAllChats();
                if (listener != null) listener.onDataLoaded(chats);
            } catch (Exception e) {
                if (listener != null) listener.onError(e.getMessage());
            }
        });
    }

    // =====================================================================
    // NOTIFICATIONS
    // =====================================================================


    // =====================================================================
    // CALLBACK INTERFACES
    // =====================================================================

    public interface OnSuccessListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnDataLoadedListener<T> {
        void onDataLoaded(T data);
        void onError(String error);
    }
}
