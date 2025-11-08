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
    List<ProjectMemberEntity> getMembersByProject(String projectId);

    // ✅ Kiểm tra 1 user có trong project không
    @Query("SELECT * FROM project_members WHERE projectId = :projectId AND userId = :userId LIMIT 1")
    ProjectMemberEntity findMemberInProject(String projectId, String userId);

    // ✅ insert (cũ), vẫn giữ lại để không lỗi code cũ
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProjectMemberEntity member);

    // ✅ get all projects a user belongs to
    @Query("SELECT * FROM project_members WHERE userId = :userId")
    List<ProjectMemberEntity> getMembersByUser(String userId);

    // ✅ update (giữ nguyên)
    @Update
    void update(ProjectMemberEntity member);

    // ✅ delete toàn bộ member của 1 project
    @Query("DELETE FROM project_members WHERE projectId = :projectId")
    void deleteByProject(String projectId);

    // ✅ UP SERT chính thức (teamListActivity cần)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ProjectMemberEntity member);
}
