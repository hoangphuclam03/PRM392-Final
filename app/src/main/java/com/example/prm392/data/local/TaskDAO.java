package com.example.prm392.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.prm392.models.TaskEntity;

import java.util.List;

@Dao
public interface TaskDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    default void insertOrUpdate(TaskEntity task) {
        // Automatically bump updatedAt whenever the record changes
        task.updatedAt = System.currentTimeMillis();
        task.pendingSync = true;
        insertInternal(task);
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertInternal(TaskEntity task);


    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY dueDate ASC")
    List<TaskEntity> getTasksByProject(String projectId);

    @Query("SELECT * FROM tasks WHERE assignedTo = :userId")
    List<TaskEntity> getTasksByUser(String userId);

    @Query("SELECT * FROM tasks WHERE taskId = :taskId LIMIT 1")
    TaskEntity getTaskById(String taskId);

    @Query("SELECT * FROM tasks WHERE isPendingSync = 1")
    List<TaskEntity> getPendingSyncTasks();

    @Query("SELECT * FROM tasks WHERE dueDate BETWEEN :startDate AND :endDate ORDER BY dueDate ASC")
    List<TaskEntity> getTasksBetweenDates(String startDate, String endDate);

    @Query("UPDATE tasks SET status = :status WHERE taskId = :taskId")
    void updateTaskStatus(String taskId, String status);

    @Query("SELECT * FROM tasks ORDER BY dueDate DESC")
    List<TaskEntity> getAllTasks();

    @Query("UPDATE tasks SET isPendingSync = 0, lastSyncedAt = :timestamp WHERE taskId = :taskId")
    void markSynced(String taskId, long timestamp);

    @Query("SELECT * FROM tasks " +
            "WHERE projectId = :projectId " +
            "AND assignedTo = :userId " +
            "AND dueDate BETWEEN :start AND :end")
    List<TaskEntity> getMyProjectTasksBetweenDates(
            String projectId,
            String userId,
            String start,
            String end
    );

    @Delete
    void delete(TaskEntity task);

    @Query("DELETE FROM tasks")
    void clearAll();
}
