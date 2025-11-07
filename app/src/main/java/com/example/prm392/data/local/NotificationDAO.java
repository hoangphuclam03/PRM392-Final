package com.example.prm392.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.prm392.models.NotificationEntity;

import java.util.List;

@Dao
public interface NotificationDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(NotificationEntity notif);

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    List<NotificationEntity> getByUser(String userId);

    @Query("SELECT * FROM notifications WHERE isRead = 0 AND userId = :userId")
    List<NotificationEntity> getUnread(String userId);

    @Query("UPDATE notifications SET isRead = 1 WHERE notifId = :notifId")
    void markAsRead(String notifId);

    @Query("DELETE FROM notifications")
    void clearAll();
}
