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

    @Query("SELECT * FROM orders ORDER BY orderId ASC")
    List<OrderEntity> getAllOrdersSync();

    @Query("SELECT * FROM orders WHERE customerId = :customerId")
    LiveData<List<OrderEntity>> getOrdersByCustomerId(long customerId);

    @Query("SELECT * FROM orders WHERE userId = :userId")
    LiveData<List<OrderEntity>> getOrdersByUserId(long userId);

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    LiveData<List<OrderItemEntity>> getOrderItems(long orderId);

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    List<OrderItemEntity> getItemsForOrderSync(long orderId);

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    LiveData<OrderEntity> getOrderById(long orderId);

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    OrderEntity getOrderByIdSync(long orderId);

    @Query("SELECT SUM((oi.sellPrice - oi.buyPrice) * oi.quantity) AS profit " +
            "FROM order_items oi " +
            "JOIN orders o ON oi.orderId = o.orderId " +
            "WHERE o.userId = :userId")
    double calculateProfitByUserSync(long userId);

    @Query("SELECT SUM((oi.sellPrice - oi.buyPrice) * oi.quantity) AS profit " +
            "FROM order_items oi " +
            "JOIN orders o ON oi.orderId = o.orderId " +
            "WHERE o.userId = :userId AND o.customerId = :customerId")
    double calculateProfitByUserAndCustomerSync(long userId, long customerId);

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
