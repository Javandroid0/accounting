package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.model.ProductEntity;
import com.javandroid.accounting_app.data.repository.OrderRepository;
import com.javandroid.accounting_app.data.repository.OrderStateRepository;

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
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CurrentOrderViewModel(@NonNull Application application) {
        super(application);
        orderRepository = new OrderRepository(application);
        stateRepository = OrderStateRepository.getInstance();

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
                item.setQuantity(newQuantity);

                Log.d(TAG, "Updated quantity for product " + product.getName() +
                        " (ID=" + product.getProductId() + ") from " +
                        (newQuantity - quantity) + " to " + newQuantity);

                // Update total in current order
                OrderEntity order = stateRepository.getCurrentOrderValue();
                if (order != null) {
                    order.setTotal(order.getTotal() + (quantity * product.getSellPrice()));
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
                    tempId + " and quantity=" + quantity);

            // Update total in current order
            OrderEntity order = stateRepository.getCurrentOrderValue();
            if (order != null) {
                order.setTotal(order.getTotal() + (quantity * product.getSellPrice()));
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

            orderRepository.insertOrderAndGetId(order, orderId -> {
                // Save any pending changes to existing items first
                for (OrderItemEntity item : items) {
                    // Update existing items in the database
                    if (item.getOrderId() != null && item.getOrderId() > 0) {
                        orderRepository.updateOrderItem(item);
                    } else {
                        // Set order ID for new items
                        item.setOrderId(orderId);
                        orderRepository.insertOrderItem(item);
                    }
                }

                // Clear current order
                resetCurrentOrder();

                // Clear this customer's saved order state since it's now confirmed
                stateRepository.clearStoredOrderForCustomer(customerId);

                // Also clear any other customer's cached orders to prevent state leakage
                stateRepository.clearAllStoredOrders();
            });
        }
    }

    /**
     * Confirms the order and executes a callback after the order has been saved
     */
    public void confirmOrderAndThen(Runnable callback) {
        OrderEntity order = stateRepository.getCurrentOrderValue();
        List<OrderItemEntity> items = stateRepository.getCurrentOrderItemsValue();

        if (order != null && items != null && !items.isEmpty()) {
            // Get current customer ID to clear state later
            final long customerId = order.getCustomerId();

            orderRepository.insertOrderAndGetId(order, orderId -> {
                Log.d(TAG, "Order confirmed with ID: " + orderId);

                // Set the order ID in the current order (for printing)
                order.setOrderId(orderId);
                stateRepository.setCurrentOrder(order);

                // Save any pending changes to existing items first
                for (OrderItemEntity item : items) {
                    // Update existing items in the database
                    if (item.getOrderId() != null && item.getOrderId() > 0) {
                        orderRepository.updateOrderItem(item);
                    } else {
                        // Set order ID for new items
                        item.setOrderId(orderId);
                        orderRepository.insertOrderItem(item);
                    }
                }

                // Execute the callback (e.g., for printing)
                if (callback != null) {
                    callback.run();
                }

                // After the callback is done, create a new order
                resetCurrentOrder();

                // Clear this customer's saved order state since it's now confirmed
                stateRepository.clearStoredOrderForCustomer(customerId);

                // Also clear any other customer's cached orders to prevent state leakage
                stateRepository.clearAllStoredOrders();
            });
        } else {
            // If there's no valid order, just run the callback
            if (callback != null) {
                callback.run();
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
            double total = 0.0;

            // Sum up the price of all items (quantity * sellPrice)
            for (OrderItemEntity item : items) {
                total += item.getQuantity() * item.getSellPrice();
            }

            // Ensure total is never negative - for business purposes
            if (total < 0) {
                Log.w(TAG, "Calculated negative total: " + total + ". Setting to 0.");
                total = 0.0;
            }

            // Update the order total
            order.setTotal(total);
            stateRepository.setCurrentOrder(order);
        }
    }

    /**
     * Reset the current order to a new empty order
     */
    public void resetCurrentOrder() {
        // Create a new empty order with current date
        String dateString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        OrderEntity newOrder = new OrderEntity(dateString, 0.0, 0L, stateRepository.getCurrentUserId());

        // Explicitly set orderId to 0 to ensure it's treated as a new order
        newOrder.setOrderId(0);

        Log.d(TAG, "Resetting current order to new empty order");

        // Set the new order
        stateRepository.setCurrentOrder(newOrder);

        // Reset order items to a completely new empty list
        stateRepository.setCurrentOrderItems(new ArrayList<>());
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