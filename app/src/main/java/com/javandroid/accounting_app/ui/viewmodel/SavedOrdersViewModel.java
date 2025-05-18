package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.repository.OrderRepository;
import com.javandroid.accounting_app.data.repository.OrderStateRepository;
import com.javandroid.accounting_app.data.repository.OrderSessionManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel responsible for managing saved/historical orders
 */
public class SavedOrdersViewModel extends AndroidViewModel {
    private static final String TAG = "SavedOrdersViewModel";

    private final OrderRepository orderRepository;
    private final OrderSessionManager sessionManager;
    private OrderStateRepository stateRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SavedOrdersViewModel(@NonNull Application application) {
        super(application);
        orderRepository = new OrderRepository(application);
        sessionManager = OrderSessionManager.getInstance();
        stateRepository = sessionManager.getCurrentRepository();
    }

    /**
     * Gets the current state repository from the session manager
     * This ensures we're always using the most up-to-date repository
     */
    private OrderStateRepository getStateRepository() {
        // Update the reference to the current repository
        stateRepository = sessionManager.getCurrentRepository();
        return stateRepository;
    }

    /**
     * Get all orders from the repository
     */
    public LiveData<List<OrderEntity>> getAllOrders() {
        return orderRepository.getAllOrders();
    }

    /**
     * Get orders for a specific customer
     */
    public LiveData<List<OrderEntity>> getOrdersByCustomerId(long customerId) {
        return orderRepository.getOrdersByCustomerId(customerId);
    }

    /**
     * Get orders for a specific user
     */
    public LiveData<List<OrderEntity>> getOrdersByUserId(long userId) {
        return orderRepository.getOrdersByUserId(userId);
    }

    /**
     * Get an order by ID
     */
    public LiveData<OrderEntity> getOrderById(long orderId) {
        return orderRepository.getOrderById(orderId);
    }

    /**
     * Get items for a specific order
     */
    public LiveData<List<OrderItemEntity>> getOrderItems(long orderId) {
        return orderRepository.getOrderItems(orderId);
    }

    /**
     * Delete an order
     */
    public void deleteOrder(OrderEntity order) {
        orderRepository.deleteOrder(order);
    }

    /**
     * Delete all orders
     */
    public void deleteAllOrders() {
        orderRepository.deleteAllOrders();
    }

    /**
     * Delete an order item
     */
    public void deleteOrderItem(OrderItemEntity orderItem) {
        orderRepository.deleteOrderItem(orderItem);
    }

    /**
     * Delete an order and all its items
     */
    public void deleteOrderAndItems(long orderId) {
        Log.d(TAG, "Deleting order ID: " + orderId + " and all its items from database");

        executor.execute(() -> {
            try {
                // Get the order entity from the database
                OrderEntity orderToDelete = orderRepository.getOrderByIdSync(orderId);
                if (orderToDelete != null) {
                    // Delete the order from database
                    orderRepository.deleteOrder(orderToDelete);
                    Log.d(TAG, "Order deleted from database: " + orderId);
                } else {
                    Log.w(TAG, "Could not find order to delete: " + orderId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete order: " + e.getMessage(), e);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Shut down the executor when ViewModel is cleared
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }
}