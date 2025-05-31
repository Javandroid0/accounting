package com.javandroid.accounting_app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.javandroid.accounting_app.data.model.OrderItemEntity;

import java.util.List;

@Dao
public interface OrderItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrderItem(OrderItemEntity orderItem);

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    LiveData<List<OrderItemEntity>> getOrderItems(long orderId);

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    List<OrderItemEntity> getItemsForOrderSync(long orderId);

    @Update
    void updateOrderItem(OrderItemEntity orderItem);

    @Delete
    void deleteOrderItem(OrderItemEntity orderItem);

    @Query("DELETE FROM order_items")
    void deleteAllOrderItems();

    @Query("SELECT * FROM order_items WHERE itemId = :itemId LIMIT 1")
    OrderItemEntity getItemByIdSync(long itemId); // Example: New utility method if needed

    @Query("SELECT * FROM order_items")
    LiveData<List<OrderItemEntity>> getAllOrderItems(); // Example: New utility method if needed
}