package com.javandroid.accounting_app.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.javandroid.accounting_app.data.database.AppDatabase;
import com.javandroid.accounting_app.data.model.Order;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderRepository {

    private final AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public OrderRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public void insert(Order order) {
        executor.execute(() -> db.orderDao().insert(order));
    }

    public LiveData<List<Order>> getAllOrders() {
        return db.orderDao().getAllOrders();
    }

    public LiveData<List<Order>> getOrdersByUserId(String userId) {
        return db.orderDao().getOrdersByUserId(userId);
    }

    public void update(Order order) {
        executor.execute(() -> db.orderDao().update(order));
    }

    public void delete(Order order) {
        executor.execute(() -> db.orderDao().delete(order));
    }

    public void deleteAll() {
        executor.execute(() -> db.orderDao().deleteAll());
    }
}
