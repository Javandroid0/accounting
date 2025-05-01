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
    LiveData<List<Order>> getOrdersByUserId(String userId); // Filtered Read


    @Update
    void update(Order order); // Update

    @Delete
    void delete(Order order); // Delete

    @Delete
    void deleteOrder(Order order);

    @Query("DELETE FROM orders")
    void deleteAll(); // Optional
}
