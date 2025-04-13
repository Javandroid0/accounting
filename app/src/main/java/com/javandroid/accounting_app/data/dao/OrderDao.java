package com.javandroid.accounting_app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;

import com.javandroid.accounting_app.data.model.Order;

import java.util.List;

@Dao
public interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Order order); // Create

    @Query("SELECT * FROM orders")
    LiveData<List<Order>> getAllOrders(); // Read all

    @Query("SELECT * FROM orders WHERE userId = :userId")
    LiveData<List<Order>> getOrdersByUserId(long userId); // Filtered Read

    @Update
    void update(Order order); // Update

    @Delete
    void delete(Order order); // Delete

    @Query("DELETE FROM orders")
    void deleteAll(); // Optional
}
