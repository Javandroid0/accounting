package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.model.ProductEntity;
import com.javandroid.accounting_app.data.repository.OrderRepository;
import com.javandroid.accounting_app.data.repository.OrderStateRepository;
import com.javandroid.accounting_app.data.repository.OrderSessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel responsible for managing the current order being edited
 */
public class CurrentOrderViewModel extends AndroidViewModel {
    private static final String TAG = "CurrentOrderViewModel";

    private final OrderRepository orderRepository;
    private final OrderStateRepository stateRepository;
    private final OrderSessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private OrderRepository.OnOrderIdResultCallback onOrderIdResultCallback;

    public CurrentOrderViewModel(@NonNull Application application) {
        super(application);
        orderRepository = new OrderRepository(application);
        sessionManager = OrderSessionManager.getInstance();
        stateRepository = sessionManager.getCurrentRepository();

        // Initialize if empty
        if (stateRepository.getCurrentOrderValue() == null) {
            resetCurrentOrder();
        }
    }

    /**
     * Get the current order being edited
     */
    public LiveData<OrderEntity> getCurrentOrder() {
        return stateRepository.getCurrentOrder();
    }

    /**
     * Get the items for the current order
     */
    public LiveData<List<OrderItemEntity>> getCurrentOrderItems() {
        return stateRepository.getCurrentOrderItems();
    }

    /**
     * Add a product to the current order
     */
    public void addProduct(ProductEntity product, double quantity) {
        List<OrderItemEntity> items = stateRepository.getCurrentOrderItemsValue();
        if (items == null) {
            items = new ArrayList<>();
        }

        // Create a new list to trigger observer updates
        List<OrderItemEntity> newItems = new ArrayList<>(items);

        // Check if product already exists in the order
        boolean productExists = false;
        for (OrderItemEntity item : newItems) {
            if (item.getProductId() != null && item.getProductId() == product.getProductId()) {
                // Product already exists, update quantity instead of adding new item
                double newQuantity = item.getQuantity() + quantity;
                double oldQuantity = item.getQuantity();
                item.setQuantity(newQuantity);

                Log.d(TAG, "Updated quantity for product " + product.getName() +
                        " (ID=" + product.getProductId() + ") from " +
                        oldQuantity + " to " + newQuantity + ", unit price=" + product.getSellPrice());

                // Update total in current order
                OrderEntity order = stateRepository.getCurrentOrderValue();
                if (order != null) {
                    double oldTotal = order.getTotal();
                    double addedValue = quantity * product.getSellPrice();
                    order.setTotal(oldTotal + addedValue);

                    Log.d(TAG, "Order total updated: " + oldTotal + " + " + addedValue + " = " + order.getTotal() +
                            " (added " + quantity + " units of " + product.getName() + ")");

                    stateRepository.setCurrentOrder(order);
                }

                productExists = true;
                break;
            }
        }

        // If product doesn't exist, add new item
        if (!productExists) {
            // Generate a temporary unique ID
            long tempId = stateRepository.generateTempId();

            OrderItemEntity orderItem = new OrderItemEntity(tempId, product.getBarcode());
            orderItem.setProductId(product.getProductId());
            orderItem.setProductName(product.getName());
            orderItem.setBuyPrice(product.getBuyPrice());
            orderItem.setSellPrice(product.getSellPrice());
            orderItem.setQuantity(quantity);

            Log.d(TAG, "Added new product " + product.getName() +
                    " (ID=" + product.getProductId() + ") with temp itemId=" +
                    tempId + ", quantity=" + quantity + ", sell price=" + product.getSellPrice());

            // Update total in current order
            OrderEntity order = stateRepository.getCurrentOrderValue();
            if (order != null) {
                double oldTotal = order.getTotal();
                double addedValue = quantity * product.getSellPrice();
                order.setTotal(oldTotal + addedValue);

                Log.d(TAG, "Order total updated: " + oldTotal + " + " + addedValue + " = " + order.getTotal() +
                        " (added new product " + product.getName() + ")");

                stateRepository.setCurrentOrder(order);
            }

            newItems.add(orderItem);
        }

        // Set the new list to trigger observers
        stateRepository.setCurrentOrderItems(newItems);
    }

    /**
     * Remove an item from the current order
     */
    public void removeItem(OrderItemEntity item) {
        List<OrderItemEntity> items = stateRepository.getCurrentOrderItemsValue();
        if (items == null) {
            return;
        }

        // Update total in current order
        OrderEntity order = stateRepository.getCurrentOrderValue();
        if (order != null) {
            double itemTotal = item.getQuantity() * item.getSellPrice();
            double newTotal = order.getTotal() - itemTotal;

            // Ensure total is never negative
            if (newTotal < 0) {
                newTotal = 0.0;
            }

            order.setTotal(newTotal);
            stateRepository.setCurrentOrder(order);
        }

        // Update UI by removing from list and notifying
        boolean removed = false;

        // First try direct object removal
        if (items.remove(item)) {
            removed = true;
        } else {
            // If direct object removal fails, try finding by ID
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getItemId() == item.getItemId()) {
                    items.remove(i);
                    removed = true;
                    break;
                }
            }
        }

        // Force the list to be considered as new to trigger UI updates
        List<OrderItemEntity> newList = new ArrayList<>(items);
        stateRepository.setCurrentOrderItems(newList);
    }

    /**
     * Update the quantity of an item in the current order
     */
    public void updateQuantity(OrderItemEntity item, double newQuantity) {
        List<OrderItemEntity> items = stateRepository.getCurrentOrderItemsValue();
        if (items == null) {
            items = new ArrayList<>();
            stateRepository.setCurrentOrderItems(items);
            return;
        }

        boolean itemFound = false;
        for (OrderItemEntity orderItem : items) {
            if (orderItem.getItemId() == item.getItemId()) {
                // Calculate the price difference based on the quantity change
                double priceDiff = (newQuantity - orderItem.getQuantity()) * orderItem.getSellPrice();
                orderItem.setQuantity(newQuantity);

                Log.d(TAG, "Updated quantity for item " + orderItem.getProductName() +
                        " (ID=" + orderItem.getItemId() + ") from " +
                        item.getQuantity() + " to " + newQuantity);

                // Update total in current order
                OrderEntity order = stateRepository.getCurrentOrderValue();
                if (order != null) {
                    double newTotal = order.getTotal() + priceDiff;
                    if (newTotal < 0) {
                        // Recalculate the total based on all items to ensure correctness
                        updateOrderTotal();
                    } else {
                        order.setTotal(newTotal);
                        stateRepository.setCurrentOrder(order);
                    }
                }

                itemFound = true;
                break;
            }
        }

        if (itemFound) {
            // Force the list to be considered as new to trigger UI updates
            List<OrderItemEntity> newList = new ArrayList<>(items);
            stateRepository.setCurrentOrderItems(newList);
        } else {
            Log.w(TAG, "Tried to update quantity for non-existent item: " + item.getProductName() +
                    " (ID=" + item.getItemId() + ")");
        }
    }

    /**
     * Confirm the current order, saving it to the database
     */
    public void confirmOrder() {
        OrderEntity order = stateRepository.getCurrentOrderValue();
        List<OrderItemEntity> items = stateRepository.getCurrentOrderItemsValue();

        if (order != null && items != null && !items.isEmpty()) {
            // Get current customer ID to clear state later
            final long customerId = order.getCustomerId();
            final long userId = order.getUserId();
            
            Log.d(TAG, "Confirming order - customer=" + customerId + ", user=" + userId + 
                ", total=" + order.getTotal() + ", items=" + items.size());

            // Create a final reference to the items to be used in callback
            final List<OrderItemEntity> finalItems = new ArrayList<>(items);
            
            orderRepository.insertOrderAndGetId(order, new OrderRepository.OnOrderIdResultCallback() {
                @Override
                public void onResult(long orderId) {
                    if (orderId > 0) {
                        Log.d(TAG, "Order saved with ID: " + orderId + ", now saving order items");
                        
                        // Save any pending changes to existing items first
                        for (OrderItemEntity item : finalItems) {
                            // Update existing items in the database
                            if (item.getOrderId() != null && item.getOrderId() > 0) {
                                Log.d(TAG, "Updating existing order item: " + item.getItemId() + 
                                    ", product=" + item.getProductName() + 
                                    ", quantity=" + item.getQuantity());
                                orderRepository.updateOrderItem(item);
                            } else {
                                // Set order ID for new items
                                Log.d(TAG, "Setting orderId=" + orderId + " for item: " + item.getItemId() + 
                                    ", product=" + item.getProductName() + 
                                    ", quantity=" + item.getQuantity());
                                item.setOrderId(orderId);
                                orderRepository.insertOrderItem(item);
                            }
                        }

                        // We're in a background thread here, need to handle this properly
                        // Instead of resetting, create a new session
                        mainHandler.post(() -> {
                            Log.d(TAG, "Creating new order session for user: " + userId);
                            // Create a completely new repository for the next order
                            sessionManager.createNewSession(userId);
                            
                            // Instead of reassigning stateRepository, access the current one from the session manager
                            // and update its values directly
                            OrderStateRepository currentRepo = sessionManager.getCurrentRepository();
                            
                            // Get the new empty order and items
                            OrderEntity newEmptyOrder = currentRepo.getCurrentOrderValue();
                            List<OrderItemEntity> newEmptyItems = currentRepo.getCurrentOrderItemsValue();
                            
                            Log.d(TAG, "New order session created - orderId=" + 
                                (newEmptyOrder != null ? newEmptyOrder.getOrderId() : "null") + 
                                ", userId=" + (newEmptyOrder != null ? newEmptyOrder.getUserId() : "null") + 
                                ", customerId=" + (newEmptyOrder != null ? newEmptyOrder.getCustomerId() : "null") + 
                                ", total=" + (newEmptyOrder != null ? newEmptyOrder.getTotal() : "null"));
                            
                            // Update our state repository with these values
                            if (newEmptyOrder != null) {
                                stateRepository.setCurrentOrder(newEmptyOrder);
                            }
                            if (newEmptyItems != null) {
                                stateRepository.setCurrentOrderItems(newEmptyItems);
                            }
                        });
                    } else {
                        Log.e(TAG, "Failed to insert order, returned ID was 0 or negative");
                        mainHandler.post(() -> {
                            Toast.makeText(getApplication(), "Error saving order", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        } else {
            Log.w(TAG, "Cannot confirm order: " + 
                (order == null ? "order is null" : (items == null ? "items is null" : "items is empty")));
        }
    }

    /**
     * Confirms the order and executes a callback after the order has been saved
     */
    public void confirmOrderAndThen(Runnable callback) {
        OrderEntity order = stateRepository.getCurrentOrderValue();
        List<OrderItemEntity> items = stateRepository.getCurrentOrderItemsValue();

        if (order != null && items != null && !items.isEmpty()) {
            // Get current user ID to create new session later
            final long userId = order.getUserId();
            final long customerId = order.getCustomerId();
            
            Log.d(TAG, "Confirming order (with callback) - customer=" + customerId + 
                ", user=" + userId + ", total=" + order.getTotal() + ", items=" + items.size());

            // Create a final reference to the items to be used in callback
            final List<OrderItemEntity> finalItems = new ArrayList<>(items);
            final Runnable safeCallback = callback; // Keep a final reference
            
            orderRepository.insertOrderAndGetId(order, new OrderRepository.OnOrderIdResultCallback() {
                @Override
                public void onResult(long orderId) {
                    if (orderId > 0) {
                        Log.d(TAG, "Order confirmed with ID: " + orderId);

                        // Set the order ID in the current order (for printing)
                        order.setOrderId(orderId);
                        stateRepository.setCurrentOrder(order);

                        // Save any pending changes to existing items first
                        for (OrderItemEntity item : finalItems) {
                            // Update existing items in the database
                            if (item.getOrderId() != null && item.getOrderId() > 0) {
                                Log.d(TAG, "Updating existing order item: " + item.getItemId() + 
                                    ", product=" + item.getProductName() + 
                                    ", quantity=" + item.getQuantity());
                                orderRepository.updateOrderItem(item);
                            } else {
                                // Set order ID for new items
                                Log.d(TAG, "Setting orderId=" + orderId + " for item: " + item.getItemId() + 
                                    ", product=" + item.getProductName() + 
                                    ", quantity=" + item.getQuantity());
                                item.setOrderId(orderId);
                                orderRepository.insertOrderItem(item);
                            }
                        }

                        // Execute the callback (e.g., for printing)
                        if (safeCallback != null) {
                            Log.d(TAG, "Executing post-confirmation callback");
                            safeCallback.run();
                        } else {
                            Log.w(TAG, "Callback is null, not executing post-confirmation actions");
                        }

                        // After the callback is done, create a new session
                        mainHandler.post(() -> {
                            Log.d(TAG, "Creating new order session for user: " + userId);
                            // Create a completely new repository for the next order
                            sessionManager.createNewSession(userId);
                            
                            // Get the new repository and update our state with its values
                            OrderStateRepository currentRepo = sessionManager.getCurrentRepository();
                            OrderEntity newEmptyOrder = currentRepo.getCurrentOrderValue();
                            List<OrderItemEntity> newEmptyItems = currentRepo.getCurrentOrderItemsValue();
                            
                            Log.d(TAG, "New order session created - orderId=" + 
                                (newEmptyOrder != null ? newEmptyOrder.getOrderId() : "null") + 
                                ", userId=" + (newEmptyOrder != null ? newEmptyOrder.getUserId() : "null") + 
                                ", customerId=" + (newEmptyOrder != null ? newEmptyOrder.getCustomerId() : "null") + 
                                ", total=" + (newEmptyOrder != null ? newEmptyOrder.getTotal() : "null"));
                            
                            // Update our state repository with these values to refresh the UI
                            if (newEmptyOrder != null) {
                                stateRepository.setCurrentOrder(newEmptyOrder);
                            }
                            if (newEmptyItems != null) {
                                stateRepository.setCurrentOrderItems(newEmptyItems);
                            }
                        });
                    } else {
                        Log.e(TAG, "Failed to insert order, returned ID was 0 or negative");
                        mainHandler.post(() -> {
                            Toast.makeText(getApplication(), "Error saving order", Toast.LENGTH_SHORT).show();
                        });
                        
                        // Still call the callback if it exists
                        if (safeCallback != null) {
                            Log.d(TAG, "Executing callback despite order insert failure");
                            safeCallback.run();
                        }
                    }
                }
            });
        } else {
            Log.w(TAG, "Cannot confirm order with callback: " + 
                (order == null ? "order is null" : (items == null ? "items is null" : "items is empty")));
            
            // If there's no valid order, just run the callback
            if (callback != null) {
                Log.d(TAG, "Executing callback without order confirmation");
                callback.run();
            } else {
                Log.w(TAG, "Callback is null, nothing to do");
            }
        }
    }

    /**
     * Recalculates the total for the current order based on all order items
     */
    public void updateOrderTotal() {
        OrderEntity order = stateRepository.getCurrentOrderValue();
        List<OrderItemEntity> items = stateRepository.getCurrentOrderItemsValue();

        if (order != null && items != null) {
            double oldTotal = order.getTotal();
            double total = 0.0;

            // Sum up the price of all items (quantity * sellPrice)
            for (OrderItemEntity item : items) {
                double itemTotal = item.getQuantity() * item.getSellPrice();
                total += itemTotal;
                Log.d(TAG, "Item total calculation: " + item.getProductName() + 
                    ", quantity=" + item.getQuantity() + 
                    ", price=" + item.getSellPrice() + 
                    ", subtotal=" + itemTotal);
            }

            // Ensure total is never negative - for business purposes
            if (total < 0) {
                Log.w(TAG, "Calculated negative total: " + total + ". Setting to 0.");
                total = 0.0;
            }

            // Update the order total
            order.setTotal(total);
            Log.d(TAG, "Order total recalculated: " + oldTotal + " -> " + total + 
                " (difference: " + (total - oldTotal) + ")");
                
            stateRepository.setCurrentOrder(order);
        } else {
            Log.w(TAG, "Cannot update order total: " + 
                (order == null ? "order is null" : "items is null"));
        }
    }

    /**
     * Force refresh the items in the repository to ensure they're not lost
     * This is needed in some cases where the repository might lose items during state transitions
     * 
     * @param items The items to set in the repository
     */
    public void refreshItems(List<OrderItemEntity> items) {
        if (items == null) {
            Log.w(TAG, "Cannot refresh items: items list is null");
            return;
        }
        
        // Create a copy to ensure we don't modify the original list
        List<OrderItemEntity> itemsCopy = new ArrayList<>(items);
        Log.d(TAG, "Refreshing " + itemsCopy.size() + " items in repository");
        
        // Explicitly set the items in the repository to ensure they're not lost
        stateRepository.setCurrentOrderItems(itemsCopy);
        
        // Make sure the total is updated to reflect these items
        updateOrderTotal();
        
        // Log the state after refresh to verify
        OrderEntity order = stateRepository.getCurrentOrderValue();
        List<OrderItemEntity> currentItems = stateRepository.getCurrentOrderItemsValue();
        
        Log.d(TAG, "After refresh - items count: " + (currentItems != null ? currentItems.size() : 0) + 
            ", total: " + (order != null ? order.getTotal() : "null"));
    }

    /**
     * Reset the current order to a new empty order
     */
    public void resetCurrentOrder() {
        // Check if we're on the main thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            resetCurrentOrderInternal();
        } else {
            // We're on a background thread, need to post to main thread
            mainHandler.post(this::resetCurrentOrderInternal);
        }
    }

    /**
     * Internal implementation of resetCurrentOrder that must be called on the main thread
     */
    private void resetCurrentOrderInternal() {
        // Get current user ID
        long userId = 0;
        OrderEntity currentOrder = stateRepository.getCurrentOrderValue();
        if (currentOrder != null) {
            userId = currentOrder.getUserId();
        }

        // Create a new session with the current user ID
        sessionManager.createNewSession(userId);
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