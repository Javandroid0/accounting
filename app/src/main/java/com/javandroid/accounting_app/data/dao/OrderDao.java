package com.javandroid.accounting_app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;

import com.javandroid.accounting_app.data.model.OrderEntity;
// OrderItemEntity import is no longer needed here for the moved methods

import java.util.List;

@Dao
public interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOrder(OrderEntity order);

    @Query("SELECT * FROM orders ORDER BY date DESC")
    LiveData<List<OrderEntity>> getAllOrders();

    @Query("SELECT * FROM orders ORDER BY orderId ASC")
    List<OrderEntity> getAllOrdersSync();

    @Query("SELECT * FROM orders WHERE customerId = :customerId")
    LiveData<List<OrderEntity>> getOrdersByCustomerId(long customerId);

    @Query("SELECT * FROM orders WHERE userId = :userId")
    LiveData<List<OrderEntity>> getOrdersByUserId(long userId);


    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    LiveData<OrderEntity> getOrderById(long orderId);

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    OrderEntity getOrderByIdSync(long orderId);

    @Query("SELECT SUM((oi.sellPrice - oi.buyPrice) * oi.quantity) AS profit " +
            "FROM order_items oi " + // Still need order_items table for this query
            "JOIN orders o ON oi.orderId = o.orderId " +
            "WHERE o.userId = :userId")
    double calculateProfitByUserSync(long userId);

    @Query("SELECT SUM((oi.sellPrice - oi.buyPrice) * oi.quantity) AS profit " +
            "FROM order_items oi " + // Still need order_items table for this query
            "JOIN orders o ON oi.orderId = o.orderId " +
            "WHERE o.userId = :userId AND o.customerId = :customerId")
    double calculateProfitByUserAndCustomerSync(long userId, long customerId);

    @Query("SELECT SUM(total) FROM orders WHERE customerId = :customerId")
    LiveData<Double> getTotalBoughtByCustomer(long customerId);


    @Update
    void updateOrder(OrderEntity order);

    @Delete
    void deleteOrder(OrderEntity order);

    @Query("DELETE FROM orders")
    void deleteAllOrders();

    @Query("SELECT * FROM orders ORDER BY total DESC")
    LiveData<List<OrderEntity>> getAllOrdersSortedByTotal();

    @Query("SELECT * FROM orders ORDER BY customerId ASC, orderId DESC")
    LiveData<List<OrderEntity>> getAllOrdersSortedByCustomer();

    @Query("SELECT MAX(orderId) FROM orders")
    long getLastOrderIdSync();
}
