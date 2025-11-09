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

    // ✅ Lấy tất cả member trong 1 project
    @Query("SELECT * FROM project_members WHERE projectId = :projectId")
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ProjectMemberEntity member);

    @Query("SELECT * FROM project_members WHERE projectId = :projectId ORDER BY fullName ASC")
    List<ProjectMemberEntity> getMembersByProject(String projectId);

    // ✅ Kiểm tra 1 user có trong project không
    @Query("SELECT * FROM project_members WHERE projectId = :projectId AND userId = :userId LIMIT 1")
    ProjectMemberEntity findMemberInProject(String projectId, String userId);
    @Query("SELECT * FROM project_members WHERE userId = :userId")
    List<ProjectMemberEntity> getMembershipsByUser(String userId);

    // ✅ insert (cũ), vẫn giữ lại để không lỗi code cũ
    @Query("DELETE FROM project_members WHERE projectId = :projectId AND userId = :userId")
    void removeMember(String projectId, String userId);

    @Query("DELETE FROM project_members")
    void clearAll();
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProjectMemberEntity member);

    // ✅ get all projects a user belongs to
    @Query("SELECT * FROM project_members WHERE userId = :userId")
    List<ProjectMemberEntity> getMembersByUser(String userId);
    @Query("SELECT * FROM project_members WHERE pendingSync = 1")
    List<ProjectMemberEntity> getPendingMembers();

    // ✅ update (giữ nguyên)
    @Update
    void update(ProjectMemberEntity member);
    @Query("UPDATE project_members SET pendingSync = 0 WHERE memberId = :memberId")
    void markSynced(String memberId);

    // ✅ delete toàn bộ member của 1 project

    @Query("DELETE FROM project_members WHERE projectId=:projectId")
    void deleteByProject(String projectId);


    // ✅ UP SERT chính thức (teamListActivity cần)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ProjectMemberEntity member);
}
