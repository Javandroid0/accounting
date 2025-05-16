package com.javandroid.accounting_app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;

import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;

import java.util.List;

@Dao
public interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOrder(OrderEntity order);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrderItem(OrderItemEntity orderItem);

    @Query("SELECT * FROM orders ORDER BY orderId ASC")
    LiveData<List<OrderEntity>> getAllOrders();

    @Query("SELECT * FROM orders WHERE customerId = :customerId")
    LiveData<List<OrderEntity>> getOrdersByCustomerId(long customerId);

    @Query("SELECT * FROM orders WHERE userId = :userId")
    LiveData<List<OrderEntity>> getOrdersByUserId(long userId);

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    LiveData<List<OrderItemEntity>> getOrderItems(long orderId);

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    LiveData<OrderEntity> getOrderById(long orderId);

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    OrderEntity getOrderByIdSync(long orderId);

    @Update
    void updateOrder(OrderEntity order);

    @Update
    void updateOrderItem(OrderItemEntity orderItem);

    @Delete
    void deleteOrder(OrderEntity order);

    @Delete
    void deleteOrderItem(OrderItemEntity orderItem);

    @Query("DELETE FROM orders")
    void deleteAllOrders();

    @Query("DELETE FROM order_items")
    void deleteAllOrderItems();
}
