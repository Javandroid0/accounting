package com.javandroid.accounting_app.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.javandroid.accounting_app.data.database.AppDatabase;
import com.javandroid.accounting_app.data.dao.OrderItemDao;
import com.javandroid.accounting_app.data.model.OrderItemEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderItemRepository {
    private static final String TAG = "OrderItemRepository";

    private final OrderItemDao orderItemDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AppDatabase db; // Keep a reference to AppDatabase if needed for transactions spanning multiple DAOs, though unlikely for this repo

    public OrderItemRepository(Context context) {
        db = AppDatabase.getInstance(context);
        orderItemDao = db.orderItemDao();
    }

    public void insertOrderItem(OrderItemEntity orderItem) {
        if (orderItem == null) {
            Log.e(TAG, "Cannot insert null order item");
            return;
        }
        if (orderItem.getOrderId() == null || orderItem.getOrderId() <= 0) {
            Log.e(TAG, "Cannot insert order item: invalid order ID: " + orderItem.getOrderId() + " for item " + orderItem.getProductName());
            // Potentially throw an error or notify via callback
            return;
        }
        Log.d(TAG, "Inserting order item: itemId=" + orderItem.getItemId()
                + ", orderId=" + orderItem.getOrderId()
                + ", productId=" + orderItem.getProductId()
                + ", quantity=" + orderItem.getQuantity()
                + ", price=" + orderItem.getSellPrice());

        executor.execute(() -> {
            // Transaction for a single item insert might be overkill unless there are related operations
            // For simplicity, direct DAO call is often fine here.
            // If transactions are needed with other DAOs, they should be managed at a higher level (e.g., UseCase or ViewModel coordinating multiple repositories)
            // or the AppDatabase instance can be used here to begin/end transactions if this repo needs to.
            try {
                orderItemDao.insertOrderItem(orderItem);
                Log.d(TAG, "Order item with ID " + orderItem.getItemId() +
                        " inserted successfully for order " + orderItem.getOrderId());
            } catch (Exception e) {
                Log.e(TAG, "Error inserting order item: " + e.getMessage(), e);
            }
        });
    }

    public LiveData<List<OrderItemEntity>> getOrderItems(long orderId) {
        Log.d(TAG, "Getting items for order: " + orderId);
        return orderItemDao.getOrderItems(orderId);
    }

    public List<OrderItemEntity> getItemsForOrderSync(long orderId) {
        Log.d(TAG, "Getting items for order (sync): " + orderId);
        return orderItemDao.getItemsForOrderSync(orderId);
    }

    public void updateOrderItem(OrderItemEntity orderItem) {
        Log.d(TAG, "Updating order item: itemId=" + orderItem.getItemId()
                + ", orderId=" + orderItem.getOrderId()
                + ", quantity=" + orderItem.getQuantity()
                + ", price=" + orderItem.getSellPrice());
        executor.execute(() -> {
            try {
                orderItemDao.updateOrderItem(orderItem);
                Log.d(TAG, "Order item updated successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error updating order item: " + e.getMessage(), e);
            }
        });
    }

    public void deleteOrderItem(OrderItemEntity orderItem) {
        Log.d(TAG, "Deleting order item: itemId=" + orderItem.getItemId()
                + ", orderId=" + orderItem.getOrderId());
        executor.execute(() -> {
            try {
                orderItemDao.deleteOrderItem(orderItem);
                Log.d(TAG, "Order item deleted successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error deleting order item: " + e.getMessage(), e);
            }
        });
    }

    public void deleteAllOrderItems() {
        Log.d(TAG, "Deleting all order items");
        executor.execute(() -> {
            try {
                orderItemDao.deleteAllOrderItems();
                Log.d(TAG, "All order items deleted successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error deleting all order items: " + e.getMessage(), e);
            }
        });
    }

    public void deleteOrderItemsByOrderId(long orderId) {
        Log.d(TAG, "Deleting all items for order ID: " + orderId);
        executor.execute(() -> {
            List<OrderItemEntity> items = orderItemDao.getItemsForOrderSync(orderId);
            if (items != null && !items.isEmpty()) {
                for (OrderItemEntity item : items) {
                    orderItemDao.deleteOrderItem(item);
                }
                Log.d(TAG, "Deleted " + items.size() + " items for order ID: " + orderId);
            } else {
                Log.d(TAG, "No items found to delete for order ID: " + orderId);
            }
        });
    }
}