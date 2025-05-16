package com.javandroid.accounting_app.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.javandroid.accounting_app.data.database.AppDatabase;
import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.model.ProductEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderRepository {

    private final AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public OrderRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public void insertOrderAndGetId(OrderEntity order, OnOrderIdResultCallback callback) {
        executor.execute(() -> {
            long orderId = db.orderDao().insertOrder(order);
            callback.onResult(orderId);
        });
    }

    // Order operations
    public void insertOrder(OrderEntity order) {
        executor.execute(() -> db.orderDao().insertOrder(order));
    }

    public void insertOrderItem(OrderItemEntity orderItem) {
        executor.execute(() -> db.orderDao().insertOrderItem(orderItem));
    }

    public LiveData<List<OrderEntity>> getAllOrders() {
        return db.orderDao().getAllOrders();
    }

    public LiveData<List<OrderEntity>> getOrdersByCustomerId(long customerId) {
        return db.orderDao().getOrdersByCustomerId(customerId);
    }

    public LiveData<List<OrderEntity>> getOrdersByUserId(long userId) {
        return db.orderDao().getOrdersByUserId(userId);
    }

    public LiveData<List<OrderItemEntity>> getOrderItems(long orderId) {
        return db.orderDao().getOrderItems(orderId);
    }

    public void getProductByBarcode(String barcode, OnProductResultCallback callback) {
        executor.execute(() -> {
            ProductEntity product = db.productDao().getProductByBarcodeSync(barcode);
            callback.onResult(product);
        });
    }

    public void updateOrder(OrderEntity order) {
        executor.execute(() -> db.orderDao().updateOrder(order));
    }

    public void updateOrderItem(OrderItemEntity orderItem) {
        executor.execute(() -> db.orderDao().updateOrderItem(orderItem));
    }

    public void deleteOrder(OrderEntity order) {
        executor.execute(() -> db.orderDao().deleteOrder(order));
    }

    public void deleteOrderItem(OrderItemEntity orderItem) {
        executor.execute(() -> db.orderDao().deleteOrderItem(orderItem));
    }

    public void deleteAllOrders() {
        executor.execute(() -> {
            db.orderDao().deleteAllOrders();
            db.orderDao().deleteAllOrderItems();
        });
    }

    public interface OnOrderIdResultCallback {
        void onResult(long orderId);
    }

    public interface OnProductResultCallback {
        void onResult(ProductEntity product);
    }
}
