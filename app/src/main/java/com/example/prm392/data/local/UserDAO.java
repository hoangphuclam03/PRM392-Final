package com.example.prm392.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.prm392.models.UserEntity;

import java.util.List;

@Dao
public interface UserDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(UserEntity user);

    @Query("SELECT * FROM users ORDER BY fullName ASC")
    List<UserEntity> getAll();

    @Query("SELECT * FROM users WHERE userId = :id LIMIT 1")
    UserEntity findById(String id);

    @Query("DELETE FROM users")
    void clearAll();
}
