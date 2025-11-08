package com.example.prm392.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.prm392.models.ProjectMemberEntity;

import java.util.List;

@Dao
public interface ProjectMemberDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ProjectMemberEntity member);

    @Query("SELECT * FROM project_members WHERE projectId = :projectId ORDER BY fullName ASC")
    List<ProjectMemberEntity> getMembersByProject(String projectId);

    @Query("SELECT * FROM project_members WHERE userId = :userId")
    List<ProjectMemberEntity> getMembershipsByUser(String userId);

    @Query("DELETE FROM project_members WHERE projectId = :projectId AND userId = :userId")
    void removeMember(String projectId, String userId);

    @Query("DELETE FROM project_members")
    void clearAll();
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProjectMemberEntity member);

    @Query("SELECT * FROM project_members WHERE pendingSync = 1")
    List<ProjectMemberEntity> getPendingMembers();

    @Query("UPDATE project_members SET pendingSync = 0 WHERE memberId = :memberId")
    void markSynced(String memberId);

    @Query("DELETE FROM project_members WHERE projectId=:projectId")
    void deleteByProject(String projectId);

}
