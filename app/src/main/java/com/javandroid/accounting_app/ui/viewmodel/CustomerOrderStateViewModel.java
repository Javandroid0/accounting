package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.repository.OrderStateRepository;
import com.javandroid.accounting_app.data.repository.OrderSessionManager;

import java.util.List;
import java.util.ArrayList;

/**
 * ViewModel responsible for managing customer-specific order state
 */
public class CustomerOrderStateViewModel extends AndroidViewModel {
    private static final String TAG = "CustomerOrderStateVM";

    private final OrderSessionManager sessionManager;
    private OrderStateRepository stateRepository;

    public CustomerOrderStateViewModel(@NonNull Application application) {
        super(application);
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
     * Set the customer ID for the current order
     * and load any saved state for that customer
     */
    public void setCustomerId(long customerId) {
        OrderStateRepository repo = getStateRepository();
        OrderEntity order = repo.getCurrentOrderValue();
        if (order != null) {
            // First, get current items before changing anything
            List<OrderItemEntity> currentItems = repo.getCurrentOrderItemsValue();

            // Log the current items and total before doing anything
            Log.d(TAG, "Current state before customer change - customerId=" + order.getCustomerId()
                    + ", items=" + (currentItems != null ? currentItems.size() : 0)
                    + ", total=" + order.getTotal());

            // Save current order state for the previous customer
            if (order.getCustomerId() > 0) {
                Log.d(TAG, "Saving state for previous customer: " + order.getCustomerId() +
                        " before switching to customer: " + customerId);
                saveCurrentOrderStateForCustomer(order.getCustomerId());
            }

            // Set new customer ID
            order.setCustomerId(customerId);
            repo.setCurrentOrder(order);

            // If we already have an order for this customer, load it
            if (repo.hasStoredOrderForCustomer(customerId)) {
                OrderEntity savedOrder = repo.getStoredOrderForCustomer(customerId);
                List<OrderItemEntity> savedItems = repo.getStoredOrderItemsForCustomer(customerId);

                Log.d(TAG, "Restoring cached order for customer: " + customerId +
                        ", cached total: " + (savedOrder != null ? savedOrder.getTotal() : "null") +
                        ", cached items: " + (savedItems != null ? savedItems.size() : 0));

                // Update with saved data but keep the same order instance to avoid
                // breaking references in the UI
                if (savedOrder != null) {
                    order.setTotal(savedOrder.getTotal());
                    repo.setCurrentOrder(order);
                }

                // Load this customer's items
                if (savedItems != null) {
                    Log.d(TAG, "Restoring " + savedItems.size() + " cached items for customer: " + customerId);
                    repo.setCurrentOrderItems(savedItems);
                } else {
                    Log.d(TAG, "No cached items for customer: " + customerId + ", using empty list");
                    repo.setCurrentOrderItems(new java.util.ArrayList<>());
                }
            } else {
                // First time seeing this customer, start with empty order
                Log.d(TAG, "First visit for customer: " + customerId + ", starting with empty order");

                // Keep the original items when setting the customer for the first time
                // This fixed the issue where items would be lost when setting the customer

                // Calculate total from the items if there are any
                if (currentItems != null && !currentItems.isEmpty()) {
                    Log.d(TAG, "Calculating total for " + currentItems.size() + " items");
                    double total = 0.0;

                    for (OrderItemEntity item : currentItems) {
                        double itemTotal = item.getQuantity() * item.getSellPrice();
                        total += itemTotal;
                        Log.d(TAG, "Item: " + item.getProductName() + ", quantity=" + item.getQuantity()
                                + ", price=" + item.getSellPrice() + ", subtotal=" + itemTotal);
                    }

                    Log.d(TAG, "Setting calculated total: " + total + " for new customer order");
                    order.setTotal(total);

                    // Update the order with the correct total
                    repo.setCurrentOrder(order);

                    // Make sure we explicitly set the current items again to trigger any observers
                    repo.setCurrentOrderItems(new ArrayList<>(currentItems));
                    Log.d(TAG, "Preserving " + currentItems.size() + " existing items for new customer");
                } else {
                    // No items, set total to 0
                    Log.d(TAG, "No items found, setting total to 0");
                    order.setTotal(0.0);

                    // Update the order with zero total
                    repo.setCurrentOrder(order);

                    // Start with empty list
                    Log.d(TAG, "No existing items, starting with empty list");
                    repo.setCurrentOrderItems(new java.util.ArrayList<>());
                }
            }

            // Final check after all operations to make sure the order total is consistent
            // with items
            List<OrderItemEntity> finalItems = repo.getCurrentOrderItemsValue();
            OrderEntity finalOrder = repo.getCurrentOrderValue();

            if (finalItems != null && !finalItems.isEmpty() && finalOrder != null) {
                double calculatedTotal = 0.0;
                for (OrderItemEntity item : finalItems) {
                    calculatedTotal += item.getQuantity() * item.getSellPrice();
                }

                if (Math.abs(calculatedTotal - finalOrder.getTotal()) > 0.001) {
                    Log.w(TAG, "Order total mismatch after customer change. Current: " + finalOrder.getTotal()
                            + ", Calculated: " + calculatedTotal + ". Fixing total.");
                    finalOrder.setTotal(calculatedTotal);
                    repo.setCurrentOrder(finalOrder);
                }

                Log.d(TAG, "Final state after customer change - customerId=" + finalOrder.getCustomerId()
                        + ", items=" + finalItems.size()
                        + ", total=" + finalOrder.getTotal());
            }
        } else {
            Log.w(TAG, "Cannot set customer ID, current order is null");
        }
    }

    /**
     * Force set the current items and total
     * This is a direct method to update the items and total when the Repository
     * access methods aren't working
     * 
     * @param items The items to set
     * @param total The total to set
     */
    public void forceSetItemsAndTotal(List<OrderItemEntity> items, double total) {
        Log.d(TAG, "Force setting " + (items != null ? items.size() : 0) + " items and total=" + total);

        OrderStateRepository repo = getStateRepository();
        OrderEntity order = repo.getCurrentOrderValue();

        if (order != null) {
            order.setTotal(total);
            repo.setCurrentOrder(order);

            if (items != null) {
                // Make a copy to avoid reference issues
                List<OrderItemEntity> itemsCopy = new ArrayList<>(items);
                repo.setCurrentOrderItems(itemsCopy);
            } else {
                repo.setCurrentOrderItems(new ArrayList<>());
            }

            Log.d(TAG, "Items and total force-set successfully");
        } else {
            Log.w(TAG, "Cannot force set items and total: order is null");
        }
    }

    /**
     * Save the current order state for a customer
     */
    private void saveCurrentOrderStateForCustomer(long customerId) {
        OrderStateRepository repo = getStateRepository();
        OrderEntity currentOrderValue = repo.getCurrentOrderValue();
        List<OrderItemEntity> currentItemsValue = repo.getCurrentOrderItemsValue();

        if (currentOrderValue != null) {
            Log.d(TAG, "Caching order for customer: " + customerId +
                    ", total: " + currentOrderValue.getTotal());
        }

        if (currentItemsValue != null) {
            Log.d(TAG, "Caching " + currentItemsValue.size() + " items for customer: " + customerId);
        } else {
            Log.d(TAG, "No items to cache for customer: " + customerId);
        }

        repo.storeOrderStateForCustomer(customerId, currentOrderValue, currentItemsValue);
    }

    /**
     * Set the current user ID
     */
    public void setCurrentUserId(long userId) {
        getStateRepository().setCurrentUserId(userId);
    }

    /**
     * Clear stored order state for a customer
     */
    public void clearCustomerOrderState(long customerId) {
        getStateRepository().clearStoredOrderForCustomer(customerId);
    }

    /**
     * Clear all stored order states
     */
    public void clearAllCustomerOrderStates() {
        getStateRepository().clearAllStoredOrders();
    }
}