package com.example.prm392.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.prm392.models.ProjectEntity;

import java.util.List;

@Dao
public interface ProjectDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ProjectEntity project);

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    List<ProjectEntity> getAllProjects();

    @Query("SELECT * FROM projects WHERE projectId = :projectId LIMIT 1")
    ProjectEntity getProjectById(String projectId);

    @Query("SELECT * FROM projects WHERE createdBy = :userId OR ownerId = :userId ORDER BY updatedAt DESC")
    List<ProjectEntity> getProjectsByUser(String userId);

    @Query("DELETE FROM projects")
    void clearAll();

    @Query("UPDATE projects SET pendingSync = 0, lastSyncedAt = :timestamp WHERE projectId = :projectId")
    void markSynced(String projectId, long timestamp);

    @Query("SELECT * FROM projects WHERE pendingSync = 1")
    List<ProjectEntity> getPendingProjects();

    // ✅ Thêm hàm này để SyncRepository có thể gọi được
    @Query("DELETE FROM projects WHERE projectId = :projectId")
    void deleteById(String projectId);

    @Delete
    void delete(ProjectEntity project);
}
