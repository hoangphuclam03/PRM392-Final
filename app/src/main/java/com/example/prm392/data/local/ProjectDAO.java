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

    // Insert or replace (auto handles duplicates by projectId)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ProjectEntity project);

    @Update
    void update(ProjectEntity project);

    // 🔍 Find project(s) by name — used for duplicate check in CreateManagingProjectActivity
    @Query("SELECT * FROM projects WHERE projectName = :name")
    List<ProjectEntity> findByName(String name);

    @Query("DELETE FROM projects")
    void clearAll();

    // 🔍 Find project by Firestore ID
    @Query("SELECT * FROM projects WHERE projectId = :id LIMIT 1")
    ProjectEntity findById(String id);

    // 🧾 Get all projects (newest first if you want to sort by localId or createdAt)
    @Query("SELECT * FROM projects ORDER BY localId DESC")
    List<ProjectEntity> getAllProjects();

    // 🗑 Delete by ID
    @Query("DELETE FROM projects WHERE projectId = :id")
    void deleteById(String id);

    @Query("SELECT * FROM projects WHERE projectId IN (:projectIds)")
    List<ProjectEntity> getProjectsByIds(List<String> projectIds);

    @Query("SELECT * FROM projects WHERE projectId = :projectId LIMIT 1")
    ProjectEntity getProjectById(String projectId);

    // 🔍 Optional: search by keyword for a global search feature later
    @Query("SELECT * FROM projects WHERE projectName LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    List<ProjectEntity> searchProjects(String query);

    @Query("SELECT * FROM projects WHERE pendingSync = 1")
    List<ProjectEntity> getPendingProjects();

    @Query("UPDATE projects SET pendingSync = 0, lastSyncedAt = :timestamp WHERE projectId = :projectId")
    void markSynced(String projectId, long timestamp);

    @Delete
    void delete(ProjectEntity project);

}
