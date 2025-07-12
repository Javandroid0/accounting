package com.javandroid.accounting_app.ui.viewmodel.order;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData; // Added for profit LiveData

import com.javandroid.accounting_app.data.model.OrderEntity;
// import com.javandroid.accounting_app.data.model.OrderItemEntity; // Not directly used here
import com.javandroid.accounting_app.data.repository.OrderRepository;
// import com.javandroid.accounting_app.data.repository.OrderStateRepository; // Not used by this VM
// import com.javandroid.accounting_app.data.repository.OrderSessionManager; // Not used by this VM

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SavedOrdersViewModel extends AndroidViewModel {
    private static final String TAG = "SavedOrdersViewModel";

    private final OrderRepository orderRepository;
    // private final OrderItemRepository orderItemRepository; // Not directly exposed
    // private final OrderSessionManager sessionManager; // Not used
    // private OrderStateRepository stateRepository; // Not used
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());


    private final MutableLiveData<Double> userProfitLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> userCustomerProfitLiveData = new MutableLiveData<>();

    public SavedOrdersViewModel(@NonNull Application application) {
        super(application);
        orderRepository = new OrderRepository(application);
        // orderItemRepository = new OrderItemRepository(application); // Only if needed internally
        // sessionManager = OrderSessionManager.getInstance();
        // stateRepository = sessionManager.getCurrentRepository();
    }

    public LiveData<List<OrderEntity>> getAllOrders() {
        return orderRepository.getAllOrders();
    }

    public LiveData<List<OrderEntity>> getOrdersByCustomerId(long customerId) {
        return orderRepository.getOrdersByCustomerId(customerId);
    }

    public LiveData<OrderEntity> getOrderById(long orderId) {
        return orderRepository.getOrderById(orderId);
    }

    /**
     * Deletes a given order. Associated order items are expected to be deleted
     * by database cascade rules.
     *
     * @param orderToDelete The order to delete.
     * @param onComplete    Callback to run on the main thread after the operation.
     */
    public void deleteOrderAndCascade(OrderEntity orderToDelete, Runnable onComplete) {
        if (orderToDelete == null || orderToDelete.getOrderId() <= 0) {
            Log.e(TAG, "Cannot delete: Invalid OrderEntity or orderId.");
            if (onComplete != null) mainThreadHandler.post(onComplete);
            return;
        }
        Log.d(TAG, "Requesting delete for Order ID: " + orderToDelete.getOrderId());
        executor.execute(() -> {
            try {
                orderRepository.deleteOrder(orderToDelete);
                Log.d(TAG, "Order ID: " + orderToDelete.getOrderId() + " deleted. Items should cascade delete.");
                if (onComplete != null) {
                    mainThreadHandler.post(onComplete);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting order ID: " + orderToDelete.getOrderId(), e);
                if (onComplete != null) {
                    mainThreadHandler.post(onComplete); // Still call complete, but an error occurred
                }
            }
        });
    }

    // Profit calculation methods (already present)
    public void calculateProfitByUser(long userId) {
        orderRepository.calculateProfitByUser(userId, profit -> {
            Log.d(TAG, "Setting profit for user " + userId + ": " + profit);
            userProfitLiveData.postValue(profit != 0 ? profit : 0.0);
        });
    }

    public LiveData<Double> getUserProfit() {
        return userProfitLiveData;
    }

    public void calculateProfitByUserAndCustomer(long userId, long customerId) {
        Log.d(TAG, "Requesting profit calculation for user " + userId + " and customer " + customerId);
        orderRepository.calculateProfitByUserAndCustomer(userId, customerId, profit -> {
            Log.d(TAG, "Setting profit for user " + userId + " and customer " + customerId + ": " + profit);
            userCustomerProfitLiveData.postValue(profit != 0 ? profit : 0.0);
        });
    }

    public LiveData<Double> getUserCustomerProfit() {
        return userCustomerProfitLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }
}