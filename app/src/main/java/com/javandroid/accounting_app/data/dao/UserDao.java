package com.javandroid.accounting_app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;

import com.javandroid.accounting_app.data.model.UserEntity;

import java.util.List;

@Dao
public interface UserDao {

    @Query("SELECT * FROM users ORDER BY userId ASC")
    LiveData<List<UserEntity>> getAllUsers();

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    UserEntity getUserByIdSync(long userId);

    @Query("SELECT * FROM users WHERE userId = :userId")
    LiveData<UserEntity> getUserById(long userId);

    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    UserEntity getUserByCredentials(String username, String password);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UserEntity user);

    @Update
    void update(UserEntity user);

    @Delete
    void delete(UserEntity user);

    @Query("DELETE FROM users")
    void deleteAll();
}
