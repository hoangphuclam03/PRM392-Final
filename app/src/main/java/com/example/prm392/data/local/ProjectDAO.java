package com.example.prm392.data.local;

import androidx.lifecycle.LiveData;
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

    default void insertOrUpdate(ProjectEntity project) {
        // Automatically update timestamp + pending flag
        project.updatedAt = System.currentTimeMillis();
        project.pendingSync = true;
        insertProject(project);
    }
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertProject(ProjectEntity project);
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

    @Query("SELECT * FROM projects WHERE projectId IN (:projectIds)")
    List<ProjectEntity> getProjectsByIds(List<String> projectIds);

    @Query("SELECT * FROM projects WHERE projectId = :projectId LIMIT 1")
    ProjectEntity getProjectById(String projectId);

    @Query("SELECT * FROM projects WHERE createdBy = :userId OR ownerId = :userId ORDER BY updatedAt DESC")
    List<ProjectEntity> getProjectsByUser(String userId);
    @Query("SELECT * FROM projects WHERE projectName LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    List<ProjectEntity> searchProjects(String query);


    @Query("UPDATE projects SET pendingSync = 0, lastSyncedAt = :timestamp WHERE projectId = :projectId")
    void markSynced(String projectId, long timestamp);

    @Query("SELECT * FROM projects WHERE pendingSync = 1")
    List<ProjectEntity> getPendingProjects();

    // ✅ Thêm hàm này để SyncRepository có thể gọi được
    @Query("DELETE FROM projects WHERE projectId = :projectId")
    void deleteById(String projectId);

    @Query("SELECT * FROM projects WHERE isPublic = 1 ORDER BY updatedAt DESC")
    List<ProjectEntity> getPublicProjects();

    @Query("SELECT * FROM projects WHERE ownerId = :ownerId")
    List<ProjectEntity> getProjectsByOwner(String ownerId);

    @Query("""
SELECT p.* FROM projects p
INNER JOIN project_members m ON p.projectId = m.projectId
WHERE m.userId = :userId
ORDER BY p.createdAt DESC
""")
    List<ProjectEntity> getAllJoinedBy(String userId);
    @Delete
    void delete(ProjectEntity project);
    @Query("SELECT * FROM projects")
    List<ProjectEntity> getAll();

}
