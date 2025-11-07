package com.example.prm392.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.prm392.models.ChatEntity;

import java.util.List;

@Dao
public interface ChatDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ChatEntity chat);

    @Query("SELECT * FROM chats WHERE projectId = :projectId ORDER BY timestamp ASC")
    List<ChatEntity> getByProject(String projectId);

    @Query("SELECT * FROM chats WHERE isPendingSync = 1")
    List<ChatEntity> getPendingSyncChats();

    @Query("DELETE FROM chats")
    void clearAll();
}
