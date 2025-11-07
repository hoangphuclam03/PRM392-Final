package com.example.prm392.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.prm392.models.ProjectEntity;

import java.util.List;

@Dao
public interface ProjectDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ProjectEntity project);

    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    List<ProjectEntity> getAll();

    @Query("SELECT * FROM projects WHERE projectId = :id LIMIT 1")
    ProjectEntity findById(String id);

    @Query("DELETE FROM projects")
    void clearAll();
}
