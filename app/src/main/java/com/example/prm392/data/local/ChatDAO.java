package com.example.prm392.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.prm392.models.ChatEntity;
import com.example.prm392.models.NotificationEntity;

import java.util.List;

@Dao
public interface ChatDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ChatEntity chat);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ChatEntity notification);
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

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    List<NotificationEntity> getAllNotifications();
}
