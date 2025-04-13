package com.javandroid.accounting_app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;

import com.javandroid.accounting_app.data.model.User;

import java.util.List;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(User user); // Create

    @Query("SELECT * FROM users")
    LiveData<List<User>> getAllUsers(); // Read all

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User getUserById(long id); // Read one

    @Update
    void update(User user); // Update

    @Delete
    void delete(User user); // Delete
}
