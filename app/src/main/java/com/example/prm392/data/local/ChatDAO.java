package com.example.prm392.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.prm392.models.ChatEntity;

import java.util.List;

@Dao
public interface ChatDAO {

    // Dùng ở chỗ khác nếu cần upsert nhanh
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ChatEntity chat);



    // ✅ DÙNG CHO GỬI TIN: cần rowId để update lại đúng bản ghi
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ChatEntity chat);

    // ✅ Cập nhật trạng thái đã đồng bộ đúng 1 dòng (tránh tạo bản ghi mới)
    @Query("UPDATE chats SET chatId = :chatId, isPendingSync = 0 WHERE localId = :localId")
    void markSynced(int localId, String chatId);
    @Query("UPDATE chats SET chatId = :chatId, isPendingSync = 0 WHERE messageId = :messageId")
    void markUploaded(String messageId, String chatId);

    @Query("SELECT * FROM chats WHERE projectId = :projectId ORDER BY timestamp ASC")
    List<ChatEntity> getByProject(String projectId);

    @Query("SELECT * FROM chats WHERE isPendingSync = 1")
    List<ChatEntity> getPendingSyncChats();

    @Query("DELETE FROM chats")
    void clearAll();

    @Query("SELECT * FROM chats ORDER BY timestamp ASC")
    List<ChatEntity> getAllChats();

    @Query("SELECT * FROM chats WHERE projectId = :projectId ORDER BY timestamp ASC")
    LiveData<List<ChatEntity>> getByProjectLive(String projectId);
}
