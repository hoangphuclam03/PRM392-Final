package com.example.prm392.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.prm392.models.ProjectMemberEntity;

import java.util.List;

@Dao
public interface ProjectMemberDAO {

    // ✅ projectId is a String (matches your entity)
    @Query("SELECT * FROM project_members WHERE projectId = :projectId")
    List<ProjectMemberEntity> getMembersByProject(String projectId);

    @Query("SELECT * FROM project_members WHERE projectId = :projectId AND userId = :userId LIMIT 1")
    ProjectMemberEntity findMemberInProject(String projectId, String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProjectMemberEntity member);

    @Query("SELECT * FROM project_members WHERE userId = :userId")
    List<ProjectMemberEntity> getMembersByUser(String userId);

    @Update
    void update(ProjectMemberEntity member);

    @Query("DELETE FROM project_members WHERE projectId = :projectId")
    void deleteByProject(String projectId);
}
