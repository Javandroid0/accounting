package com.javandroid.accounting_app.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.javandroid.accounting_app.data.database.AppDatabase;
import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.model.ProductEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderRepository {
    private static final String TAG = "OrderRepository";

    private final AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public OrderRepository(Context context) {
        db = AppDatabase.getInstance(context);
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
            try {
                // Begin explicit transaction
                db.beginTransaction();
                Log.d(TAG, "Starting transaction for order insertion");

                try {
                    // Insert the order and get the ID
                    long orderId = db.orderDao().insertOrder(order);

                    if (orderId > 0) {
                        Log.d(TAG, "Order inserted successfully into database with ID: " + orderId);
                        // Mark transaction as successful
                        db.setTransactionSuccessful();
                        Log.d(TAG, "Transaction marked as successful");

                        // Callback with the order ID
                        callback.onResult(orderId);
                    } else {
                        Log.e(TAG, "Order insertion failed - returned ID was 0 or negative");
                        callback.onResult(0);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in database transaction: " + e.getMessage(), e);
                } finally {
                    // End transaction (will rollback if not successful)
                    db.endTransaction();
                    Log.d(TAG, "Transaction ended");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error inserting order: " + e.getMessage(), e);
                callback.onResult(0);
            }
        });
    }

    // Order operations
    public void insertOrder(OrderEntity order) {
        Log.d(TAG, "Inserting order: ID=" + order.getOrderId()
                + ", customer=" + order.getCustomerId()
                + ", user=" + order.getUserId()
                + ", total=" + order.getTotal());

        executor.execute(() -> {
            try {
                db.orderDao().insertOrder(order);
                Log.d(TAG, "Order inserted successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error inserting order: " + e.getMessage(), e);
            }
        });
    }

    public void insertOrderItem(OrderItemEntity orderItem) {
        if (orderItem == null) {
            Log.e(TAG, "Cannot insert null order item");
            return;
        }

        if (orderItem.getOrderId() == null || orderItem.getOrderId() <= 0) {
            Log.e(TAG, "Cannot insert order item: invalid order ID: " + orderItem.getOrderId());
            return;
        }

        Log.d(TAG, "Inserting order item: itemId=" + orderItem.getItemId()
                + ", orderId=" + orderItem.getOrderId()
                + ", productId=" + orderItem.getProductId()
                + ", quantity=" + orderItem.getQuantity()
                + ", price=" + orderItem.getSellPrice());

        executor.execute(() -> {
            try {
                // Begin transaction
                db.beginTransaction();
                Log.d(TAG, "Starting transaction for item insertion");

                try {
                    db.orderDao().insertOrderItem(orderItem);
                    Log.d(TAG, "Order item with ID " + orderItem.getItemId() +
                            " inserted successfully for order " + orderItem.getOrderId());

                    // Mark transaction as successful
                    db.setTransactionSuccessful();
                    Log.d(TAG, "Item transaction marked as successful");
                } catch (Exception e) {
                    Log.e(TAG, "Error in item database transaction: " + e.getMessage(), e);
                } finally {
                    // End transaction (will rollback if not successful)
                    db.endTransaction();
                    Log.d(TAG, "Item transaction ended");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error inserting order item: " + e.getMessage(), e);
            }
        });
    }

    public LiveData<List<OrderEntity>> getAllOrders() {
        Log.d(TAG, "Getting all orders");
        return db.orderDao().getAllOrders();
    }

    public LiveData<List<OrderEntity>> getOrdersByCustomerId(long customerId) {
        Log.d(TAG, "Getting orders for customer: " + customerId);
        return db.orderDao().getOrdersByCustomerId(customerId);
    }

    public LiveData<List<OrderEntity>> getOrdersByUserId(long userId) {
        Log.d(TAG, "Getting orders for user: " + userId);
        return db.orderDao().getOrdersByUserId(userId);
    }

    public LiveData<List<OrderItemEntity>> getOrderItems(long orderId) {
        Log.d(TAG, "Getting items for order: " + orderId);
        return db.orderDao().getOrderItems(orderId);
    }

    public LiveData<OrderEntity> getOrderById(long orderId) {
        Log.d(TAG, "Getting order by ID: " + orderId);
        return db.orderDao().getOrderById(orderId);
    }

    public OrderEntity getOrderByIdSync(long orderId) {
        Log.d(TAG, "Getting order by ID (sync): " + orderId);
        OrderEntity order = db.orderDao().getOrderByIdSync(orderId);
        if (order != null) {
            Log.d(TAG, "Found order: ID=" + order.getOrderId()
                    + ", customer=" + order.getCustomerId()
                    + ", total=" + order.getTotal());
        } else {
            Log.d(TAG, "No order found with ID: " + orderId);
        }
        return order;
    }

    public void getProductByBarcode(String barcode, OnProductResultCallback callback) {
        Log.d(TAG, "Looking up product by barcode: " + barcode);
        executor.execute(() -> {
            ProductEntity product = db.productDao().getProductByBarcodeSync(barcode);
            if (product != null) {
                Log.d(TAG, "Found product: " + product.getName()
                        + ", ID=" + product.getProductId()
                        + ", stock=" + product.getStock());
            } else {
                Log.d(TAG, "No product found with barcode: " + barcode);
            }
            callback.onResult(product);
        });
    }

    public void updateOrder(OrderEntity order) {
        Log.d(TAG, "Updating order: ID=" + order.getOrderId()
                + ", customer=" + order.getCustomerId()
                + ", user=" + order.getUserId()
                + ", total=" + order.getTotal());

        executor.execute(() -> {
            try {
                db.orderDao().updateOrder(order);
                Log.d(TAG, "Order updated successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error updating order: " + e.getMessage(), e);
            }
        });
    }

    public void updateOrderItem(OrderItemEntity orderItem) {
        Log.d(TAG, "Updating order item: itemId=" + orderItem.getItemId()
                + ", orderId=" + orderItem.getOrderId()
                + ", quantity=" + orderItem.getQuantity()
                + ", price=" + orderItem.getSellPrice());

        executor.execute(() -> {
            try {
                db.orderDao().updateOrderItem(orderItem);
                Log.d(TAG, "Order item updated successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error updating order item: " + e.getMessage(), e);
            }
        });
    }

    public void deleteOrder(OrderEntity order) {
        Log.d(TAG, "Deleting order: ID=" + order.getOrderId());
        executor.execute(() -> {
            try {
                db.orderDao().deleteOrder(order);
                Log.d(TAG, "Order deleted successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error deleting order: " + e.getMessage(), e);
            }
        });
    }

    public void deleteOrderItem(OrderItemEntity orderItem) {
        Log.d(TAG, "Deleting order item: itemId=" + orderItem.getItemId()
                + ", orderId=" + orderItem.getOrderId());

        executor.execute(() -> {
            try {
                db.orderDao().deleteOrderItem(orderItem);
                Log.d(TAG, "Order item deleted successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error deleting order item: " + e.getMessage(), e);
            }
        });
    }

    public void deleteAllOrders() {
        Log.d(TAG, "Deleting all orders and order items");
        executor.execute(() -> {
            try {
                db.orderDao().deleteAllOrders();
                db.orderDao().deleteAllOrderItems();
                Log.d(TAG, "All orders and items deleted successfully");
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

    // Interface for returning profit results
    public interface OnProfitResultCallback {
        void onResult(double profit);
    }

    /**
     * Calculate profit generated by a specific user
     */
    public void calculateProfitByUser(long userId, OnProfitResultCallback callback) {
        Log.d(TAG, "Calculating profit for user: " + userId);
        executor.execute(() -> {
            try {
                double profit = db.orderDao().calculateProfitByUserSync(userId);
                Log.d(TAG, "Calculated profit for user " + userId + ": " + profit);
                if (callback != null) {
                    callback.onResult(profit);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calculating profit for user " + userId + ": " + e.getMessage(), e);
                if (callback != null) {
                    callback.onResult(0.0);
                }
            }
        });
    }

    /**
     * Calculate profit generated by a specific user for a specific customer
     */
    public void calculateProfitByUserAndCustomer(long userId, long customerId, OnProfitResultCallback callback) {
        Log.d(TAG, "Calculating profit for user " + userId + " with customer " + customerId);
        executor.execute(() -> {
            try {
                double profit = db.orderDao().calculateProfitByUserAndCustomerSync(userId, customerId);
                Log.d(TAG, "Calculated profit for user " + userId + " with customer " + customerId + ": " + profit);
                if (callback != null) {
                    callback.onResult(profit);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calculating profit for user " + userId + " with customer " + customerId + ": "
                        + e.getMessage(), e);
                if (callback != null) {
                    callback.onResult(0.0);
                }
            }
        });
    }

    /**
     * Calculate profit generated by a specific user (synchronous version)
     */
    public double calculateProfitByUserSync(long userId) {
        try {
            return db.orderDao().calculateProfitByUserSync(userId);
        } catch (Exception e) {
            Log.e(TAG, "Error calculating profit for user " + userId + ": " + e.getMessage(), e);
            return 0.0;
        }
    }
}
