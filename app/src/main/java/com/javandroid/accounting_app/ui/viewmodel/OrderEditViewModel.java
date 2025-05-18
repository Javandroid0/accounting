package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.repository.OrderRepository;
import com.javandroid.accounting_app.data.repository.OrderStateRepository;
import com.javandroid.accounting_app.data.repository.OrderSessionManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel responsible for handling the editing of existing orders
 */
public class OrderEditViewModel extends AndroidViewModel {
    private static final String TAG = "OrderEditViewModel";

    private final OrderRepository orderRepository;
    private final OrderSessionManager sessionManager;
    private OrderStateRepository stateRepository;
    private final CurrentOrderViewModel currentOrderViewModel;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Event to signal when order is empty/deleted
    private final MutableLiveData<Boolean> orderEmptyEvent = new MutableLiveData<>();

    public OrderEditViewModel(@NonNull Application application) {
        super(application);
        orderRepository = new OrderRepository(application);
        sessionManager = OrderSessionManager.getInstance();
        stateRepository = sessionManager.getCurrentRepository();
        currentOrderViewModel = new ViewModelProvider.AndroidViewModelFactory(application)
                .create(CurrentOrderViewModel.class);
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
     * Set an existing order for editing
     */
    public void setEditingOrder(OrderEntity order) {
        Log.d(TAG, "Setting order for editing: ID=" + order.getOrderId());
        getStateRepository().setCurrentOrder(order);

        // Automatically load the order items for this order
        if (order.getOrderId() > 0) {
            // Use a one-time observer to load the order items
            final androidx.lifecycle.Observer<List<OrderItemEntity>> observer = new androidx.lifecycle.Observer<List<OrderItemEntity>>() {
                @Override
                public void onChanged(List<OrderItemEntity> orderItems) {
                    if (orderItems != null) {
                        Log.d(TAG, "Loaded " + orderItems.size() + " items for order ID=" + order.getOrderId());
                        getStateRepository().setCurrentOrderItems(orderItems);
                    } else {
                        Log.w(TAG, "No items found for order ID=" + order.getOrderId());
                        // Set empty list instead of null
                        getStateRepository().setCurrentOrderItems(new ArrayList<>());
                    }
                    // Remove observer after use to prevent memory leaks
                    orderRepository.getOrderItems(order.getOrderId()).removeObserver(this);
                }
            };

            // Start observing
            orderRepository.getOrderItems(order.getOrderId()).observeForever(observer);
        }
    }

    /**
     * Cancel editing and reload the original order from the database
     */
    public void cancelOrderEditing() {
        OrderEntity currentOrderValue = getStateRepository().getCurrentOrderValue();
        if (currentOrderValue != null && currentOrderValue.getOrderId() > 0) {
            // This is an existing order, reload it from database to discard changes
            long orderId = currentOrderValue.getOrderId();
            Log.d(TAG, "Canceling edits for order ID: " + orderId);

            // Use a one-time observer to load the original order
            final androidx.lifecycle.Observer<OrderEntity> orderObserver = new androidx.lifecycle.Observer<OrderEntity>() {
                @Override
                public void onChanged(OrderEntity order) {
                    if (order != null) {
                        Log.d(TAG, "Reloaded order from DB: " + order.getOrderId() +
                                ", total: " + order.getTotal());
                        getStateRepository().setCurrentOrder(order);
                        // Remove observer after use to prevent memory leaks
                        orderRepository.getOrderById(orderId).removeObserver(this);

                        // Also reload items
                        final androidx.lifecycle.Observer<List<OrderItemEntity>> itemsObserver = new androidx.lifecycle.Observer<List<OrderItemEntity>>() {
                            @Override
                            public void onChanged(List<OrderItemEntity> items) {
                                if (items != null) {
                                    Log.d(TAG, "Reloaded " + items.size() + " items for order ID: " + orderId);
                                    getStateRepository().setCurrentOrderItems(items);
                                } else {
                                    Log.d(TAG, "No items loaded for order ID: " + orderId);
                                }
                                // Remove observer after use
                                orderRepository.getOrderItems(orderId).removeObserver(this);
                            }
                        };

                        // Start observing items
                        orderRepository.getOrderItems(orderId).observeForever(itemsObserver);
                    } else {
                        Log.w(TAG, "Failed to reload order: " + orderId + " (returned null)");
                    }
                }
            };

            // Start observing order
            orderRepository.getOrderById(orderId).observeForever(orderObserver);
        } else {
            // This is a new order, just reset it
            Log.d(TAG, "Canceling edits for NEW order (orderId=0)");
            currentOrderViewModel.resetCurrentOrder();
        }

        // Clear ALL cached order data
        getStateRepository().clearAllStoredOrders();
    }

    /**
     * Save changes to an existing order
     */
    public void saveOrderChanges() {
        OrderStateRepository repo = getStateRepository();
        OrderEntity order = repo.getCurrentOrderValue();
        List<OrderItemEntity> items = repo.getCurrentOrderItemsValue();

        if (order != null && order.getOrderId() > 0 && items != null) {
            Log.d(TAG, "Saving changes for order ID: " + order.getOrderId() +
                    ", items: " + items.size() +
                    ", total: " + order.getTotal());

            // First update the order itself
            orderRepository.updateOrder(order);

            // Log each item being saved
            for (OrderItemEntity item : items) {
                if (item.getOrderId() != null && item.getOrderId() > 0) {
                    // Update existing items
                    Log.d(TAG, "Updating existing item: " + item.getItemId() +
                            ", orderId: " + item.getOrderId() +
                            ", product: " + item.getProductName() +
                            ", quantity: " + item.getQuantity());
                    orderRepository.updateOrderItem(item);
                } else {
                    // This is a new item for an existing order
                    Log.d(TAG, "Adding new item to existing order: " +
                            "tempId: " + item.getItemId() +
                            ", product: " + item.getProductName() +
                            ", quantity: " + item.getQuantity());
                    item.setOrderId(order.getOrderId());
                    orderRepository.insertOrderItem(item);
                }
            }

            // Recalculate and save the total
            currentOrderViewModel.updateOrderTotal();
            orderRepository.updateOrder(order);

            Log.d(TAG, "Saved all changes for order ID: " + order.getOrderId() +
                    " with " + items.size() + " items, final total: " + order.getTotal());
        } else {
            Log.w(TAG, "Attempted to save changes without a valid order");
        }
    }

    /**
     * Remove all observers for a specific order ID
     */
    public void cleanupOrderObservers(long orderId) {
        Log.d(TAG, "Starting cleanup of observers for order ID: " + orderId);

        // Get the LiveData for this order's items
        LiveData<List<OrderItemEntity>> orderItemsLiveData = orderRepository.getOrderItems(orderId);

        // Force remove all observers to prevent leaks and repeated loading
        if (orderItemsLiveData instanceof MutableLiveData) {
            // Clear internal observers - this is a way to force cleanup
            try {
                Field observersField = LiveData.class.getDeclaredField("mObservers");
                observersField.setAccessible(true);
                Object observers = observersField.get(orderItemsLiveData);
                Method methodClear = observers.getClass().getDeclaredMethod("clear");
                methodClear.setAccessible(true);
                methodClear.invoke(observers);
                Log.d(TAG, "Successfully cleaned up observers for order ID: " + orderId);
            } catch (Exception e) {
                Log.e(TAG, "Failed to clean up observers: " + e.getMessage(), e);
            }
        } else {
            Log.w(TAG, "Could not clean observers for order ID: " + orderId +
                    " - LiveData is not MutableLiveData");
        }
    }

    /**
     * Get the event for order empty/deleted
     */
    public LiveData<Boolean> getOrderEmptyEvent() {
        return orderEmptyEvent;
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