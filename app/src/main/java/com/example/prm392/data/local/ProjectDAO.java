package com.example.prm392.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.prm392.models.ProjectEntity;

import java.util.List;

@Dao
public interface ProjectDAO {

    // ✅ Upsert chính thức (bạn cần để TeamListActivity dùng)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ProjectEntity project);

    // ✅ Hàm cũ vẫn giữ nguyên
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ProjectEntity project);

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    @Update
    void update(ProjectEntity project);

    @Query("SELECT * FROM projects WHERE projectName = :name")
    List<ProjectEntity> findByName(String name);

    @Query("DELETE FROM projects")
    void clearAll();

    @Query("SELECT * FROM projects WHERE projectId = :id LIMIT 1")
    ProjectEntity findById(String id);

    @Query("SELECT * FROM projects ORDER BY localId DESC")
    List<ProjectEntity> getAllProjects();

    @Query("DELETE FROM projects WHERE projectId = :id")
    void deleteById(String id);

    @Query("SELECT * FROM projects WHERE projectId IN (:projectIds)")
    List<ProjectEntity> getProjectsByIds(List<String> projectIds);

    @Query("SELECT * FROM projects WHERE projectId = :projectId LIMIT 1")
    ProjectEntity getProjectById(String projectId);

    @Query("SELECT * FROM projects WHERE createdBy = :userId OR ownerId = :userId ORDER BY updatedAt DESC")
    List<ProjectEntity> getProjectsByUser(String userId);
    @Query("SELECT * FROM projects WHERE projectName LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    List<ProjectEntity> searchProjects(String query);

    @Query("DELETE FROM projects")
    void clearAll();

    @Query("UPDATE projects SET pendingSync = 0, lastSyncedAt = :timestamp WHERE projectId = :projectId")
    void markSynced(String projectId, long timestamp);

    @Query("SELECT * FROM projects WHERE pendingSync = 1")
    List<ProjectEntity> getPendingProjects();

    // ✅ Thêm hàm này để SyncRepository có thể gọi được
    @Query("DELETE FROM projects WHERE projectId = :projectId")
    void deleteById(String projectId);

    @Query("SELECT * FROM projects WHERE isPublic = 1 ORDER BY updatedAt DESC")
    List<ProjectEntity> getPublicProjects();

    @Query("SELECT * FROM projects WHERE isPublic = 1 AND " +
            "(projectName LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') " +
            "ORDER BY updatedAt DESC")
    List<ProjectEntity> searchPublicProjects(String query);

    @Delete
    void delete(ProjectEntity project);

}
