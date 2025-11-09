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

    @Query("SELECT * FROM users WHERE fullName LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' ORDER BY fullName ASC")
    List<UserEntity> searchUsers(String query);
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    UserEntity getUserByEmail(String email);

    @Query("SELECT * FROM users ORDER BY lastLogin DESC")
    List<UserEntity> getAllUsers();

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    UserEntity getUserById(String userId);
    @Query("SELECT * FROM users WHERE userId = :id LIMIT 1")
    UserEntity findById(String id);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    UserEntity findByEmail(String email);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserEntity user);

    @Query("DELETE FROM users WHERE userId = :userId")
    void deleteUser(String userId);
    @Query("DELETE FROM users")
    void clearAll();
}
