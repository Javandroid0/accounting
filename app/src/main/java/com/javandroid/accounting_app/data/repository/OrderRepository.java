package com.javandroid.accounting_app.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.javandroid.accounting_app.data.database.AppDatabase;
import com.javandroid.accounting_app.data.dao.OrderDao;
// OrderItemDao is no longer directly used here for item-specific public methods
import com.javandroid.accounting_app.data.model.OrderEntity;
// OrderItemEntity is no longer directly manipulated by public methods here
import com.javandroid.accounting_app.data.model.ProductEntity; // Still needed for getProductByBarcode

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderRepository {
    private static final String TAG = "OrderRepository";

    private final AppDatabase db; // db might still be needed for transactions or other DAOs
    private final OrderDao orderDao;
    // private final OrderItemDao orderItemDao; // Removed, OrderItemRepository will handle this
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public OrderRepository(Context context) {
        db = AppDatabase.getInstance(context);
        orderDao = db.orderDao();
        // orderItemDao = db.orderItemDao(); // Removed
    }

    public void insertOrderAndGetId(OrderEntity order, OnOrderIdResultCallback callback) {
        Log.d(TAG, "Inserting order: ID=" + order.getOrderId()
                + ", customer=" + order.getCustomerId()
                + ", user=" + order.getUserId()
                + ", total=" + order.getTotal()
                + ", date=" + order.getDate());

        if (callback == null) {
            Log.e(TAG, "Cannot insert order: callback is null, operation might be lost");
            return;
        }
        if (order.getCustomerId() <= 0 || order.getUserId() <= 0) {
            Log.e(TAG, "Cannot insert order: invalid customer ID (" + order.getCustomerId() +
                    ") or user ID (" + order.getUserId() + ")");
            callback.onResult(0);
            return;
        }

        executor.execute(() -> {
            // Transaction for inserting an order might still be relevant if other DAOs were involved
            // For just inserting an OrderEntity, a transaction here isn't strictly necessary
            // unless OrderDao's insertOrder itself has complex logic.
            // AppDatabase level transaction for multiple DAO operations is preferred.
            long orderId = 0;
            try {
                db.beginTransaction(); // Example if a transaction is desired at this level
                orderId = orderDao.insertOrder(order);
                if (orderId > 0) {
                    db.setTransactionSuccessful();
                    Log.d(TAG, "Order inserted successfully into database with ID: " + orderId);
                } else {
                    Log.e(TAG, "Order insertion failed - returned ID was 0 or negative");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error inserting order: " + e.getMessage(), e);
            } finally {
                if (db.inTransaction()) { // Check if a transaction was actually started by this thread
                    db.endTransaction();
                }
                callback.onResult(orderId); // Ensure callback is called
            }
        });
    }

    public void insertOrder(OrderEntity order) {
        Log.d(TAG, "Inserting order: ID=" + order.getOrderId()
                + ", customer=" + order.getCustomerId()
                + ", user=" + order.getUserId()
                + ", total=" + order.getTotal());
        executor.execute(() -> {
            try {
                orderDao.insertOrder(order);
                Log.d(TAG, "Order inserted successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error inserting order: " + e.getMessage(), e);
            }
        });
    }

    // insertOrderItem - MOVED to OrderItemRepository

    public LiveData<List<OrderEntity>> getAllOrders() {
        Log.d(TAG, "Getting all orders (sorted by newest)");
        return orderDao.getAllOrders();
    }

    public LiveData<List<OrderEntity>> getOrdersByCustomerId(long customerId) {
        Log.d(TAG, "Getting orders for customer: " + customerId);
        return orderDao.getOrdersByCustomerId(customerId);
    }

    public LiveData<List<OrderEntity>> getOrdersByUserId(long userId) {
        Log.d(TAG, "Getting orders for user: " + userId);
        return orderDao.getOrdersByUserId(userId);
    }

    // getOrderItems (LiveData) - MOVED to OrderItemRepository

    public LiveData<OrderEntity> getOrderById(long orderId) {
        Log.d(TAG, "Getting order by ID: " + orderId);
        return orderDao.getOrderById(orderId);
    }

    public OrderEntity getOrderByIdSync(long orderId) {
        Log.d(TAG, "Getting order by ID (sync): " + orderId);
        OrderEntity order = orderDao.getOrderByIdSync(orderId);
        // Logging for found/not found...
        return order;
    }

    // This method might be better placed in ProductRepository if not order-context specific
    public void getProductByBarcode(String barcode, OnProductResultCallback callback) {
        Log.d(TAG, "Looking up product by barcode: " + barcode);
        executor.execute(() -> {
            ProductEntity product = db.productDao().getProductByBarcodeSync(barcode);
            // Logging...
            callback.onResult(product);
        });
    }

    public void updateOrder(OrderEntity order) {
        Log.d(TAG, "Updating order: ID=" + order.getOrderId() /* ... */);
        executor.execute(() -> {
            try {
                orderDao.updateOrder(order);
                Log.d(TAG, "Order updated successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error updating order: " + e.getMessage(), e);
            }
        });
    }

    // updateOrderItem - MOVED to OrderItemRepository

    public void deleteOrder(OrderEntity order) {
        Log.d(TAG, "Deleting order: ID=" + order.getOrderId());
        executor.execute(() -> {
            try {
                orderDao.deleteOrder(order); // ON DELETE CASCADE in OrderItemEntity should handle items
                Log.d(TAG, "Order deleted successfully. Associated items should be handled by CASCADE.");
            } catch (Exception e) {
                Log.e(TAG, "Error deleting order: " + e.getMessage(), e);
            }
        });
    }

    // deleteOrderItem - MOVED to OrderItemRepository

    public void deleteAllOrders() {
        Log.d(TAG, "Deleting all orders.");
        executor.execute(() -> {
            try {
                orderDao.deleteAllOrders(); // ON DELETE CASCADE should handle all order items.
                // If explicit deletion of all items is needed independently, it's in OrderItemRepository.
                Log.d(TAG, "All orders deleted. Associated items should be handled by CASCADE.");
            } catch (Exception e) {
                Log.e(TAG, "Error deleting all orders: " + e.getMessage(), e);
            }
        });
    }

    public interface OnOrderIdResultCallback {
        void onResult(long orderId);
    }

    public interface OnProductResultCallback {
        void onResult(ProductEntity product);
    }

    public interface OnProfitResultCallback {
        void onResult(double profit);
    }

    public LiveData<Double> getTotalBoughtByCustomer(long customerId) {
        return orderDao.getTotalBoughtByCustomer(customerId);
    }

    // Profit calculation methods remain as they join `orders` and `order_items`
    public void calculateProfitByUser(long userId, OnProfitResultCallback callback) {
        Log.d(TAG, "Calculating profit for user: " + userId);
        executor.execute(() -> {
            try {
                double profit = orderDao.calculateProfitByUserSync(userId);
                Log.d(TAG, "Calculated profit for user " + userId + ": " + profit);
                if (callback != null) callback.onResult(profit);
            } catch (Exception e) {
                Log.e(TAG, "Error calculating profit: " + e.getMessage(), e);
                if (callback != null) callback.onResult(0.0);
            }
        });
    }

    public void calculateProfitByUserAndCustomer(long userId, long customerId, OnProfitResultCallback callback) {
        Log.d(TAG, "Calculating profit for user " + userId + " with customer " + customerId);
        executor.execute(() -> {
            try {
                double profit = orderDao.calculateProfitByUserAndCustomerSync(userId, customerId);
                Log.d(TAG, "Calculated profit for user " + userId + ", customer " + customerId + ": " + profit);
                if (callback != null) callback.onResult(profit);
            } catch (Exception e) {
                Log.e(TAG, "Error calculating profit: " + e.getMessage(), e);
                if (callback != null) callback.onResult(0.0);
            }
        });
    }

    public double calculateProfitByUserSync(long userId) {
        try {
            return orderDao.calculateProfitByUserSync(userId);
        } catch (Exception e) {
            Log.e(TAG, "Error calculating profit (sync): " + e.getMessage(), e);
            return 0.0;
        }
    }

    public LiveData<List<OrderEntity>> getAllOrdersSortedByTotal() {
        return orderDao.getAllOrdersSortedByTotal();
    }

    public LiveData<List<OrderEntity>> getAllOrdersSortedByCustomer() {
        return orderDao.getAllOrdersSortedByCustomer();
    }
}