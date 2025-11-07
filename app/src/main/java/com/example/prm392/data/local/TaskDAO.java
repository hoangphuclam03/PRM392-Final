package com.example.prm392.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.prm392.models.TaskEntity;

import java.util.List;

@Dao
public interface TaskDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(TaskEntity task);

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY dueDate ASC")
    List<TaskEntity> getTasksByProject(String projectId);

    @Query("SELECT * FROM tasks WHERE assignedTo = :userId")
    List<TaskEntity> getTasksByUser(String userId);

    @Query("SELECT * FROM tasks WHERE isPendingSync = 1")
    List<TaskEntity> getPendingSyncTasks();

    @Query("DELETE FROM tasks")
    void clearAll();
}
