package com.javandroid.accounting_app.data.dto;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.javandroid.accounting_app.data.model.Order;

import java.util.List;

@Dao
public interface OrderDao {
    @Insert
    void insert(Order order1);

    @Query("SELECT * FROM order1 WHERE orderId = :orderId LIMIT 1")
    Order getOrderById(int orderId);

    @Query("SELECT * FROM order1")
    List<Order> getAllOrders();
}

