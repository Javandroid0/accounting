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
     * 
     * @param customerId           The customer ID to set
     * @param preserveCurrentItems If true, current items will be preserved even for
     *                             first-time customers (used during order
     *                             confirmation)
     */
    public void setCustomerId(long customerId, boolean preserveCurrentItems) {
        if (customerId <= 0) {
            Log.w(TAG, "Invalid customer ID: " + customerId + ". Operation ignored.");
            return;
        }

        OrderStateRepository repo = getStateRepository();
        if (repo == null) {
            Log.e(TAG, "Cannot set customer ID: repository is null");
            return;
        }

        OrderEntity order = repo.getCurrentOrderValue();
        if (order != null) {
            try {
                // First, get current items before changing anything
                List<OrderItemEntity> currentItems = repo.getCurrentOrderItemsValue();

                // Log the current items and total before doing anything
                Log.d(TAG, "Current state before customer change - customerId=" + order.getCustomerId()
                        + ", items=" + (currentItems != null ? currentItems.size() : 0)
                        + ", total=" + order.getTotal());

                // Save current order state for the previous customer if not preserving items
                if (!preserveCurrentItems && order.getCustomerId() > 0 && order.getCustomerId() != customerId) {
                    Log.d(TAG, "Saving state for previous customer: " + order.getCustomerId() +
                            " before switching to customer: " + customerId);
                    saveCurrentOrderStateForCustomer(order.getCustomerId());
                }

                // Set new customer ID
                order.setCustomerId(customerId);
                repo.setCurrentOrder(order);

                // If we're preserving current items (during order confirmation), don't load or
                // clear anything
                if (preserveCurrentItems) {
                    Log.d(TAG, "Preserving current items and total for customer: " + customerId +
                            " (preserveCurrentItems=true)");
                    // No need to do anything, keep current items and total
                }
                // If we already have an order for this customer and not preserving, load it
                else if (repo.hasStoredOrderForCustomer(customerId)) {
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
                    if (savedItems != null && !savedItems.isEmpty()) {
                        Log.d(TAG, "Restoring " + savedItems.size() + " cached items for customer: " + customerId);
                        repo.setCurrentOrderItems(savedItems);
                    } else {
                        Log.d(TAG, "No cached items for customer: " + customerId + ", using empty list");
                        repo.setCurrentOrderItems(new ArrayList<>());
                    }
                } else if (!preserveCurrentItems) {
                    // First time seeing this customer, start with empty order (only if not
                    // preserving items)
                    Log.d(TAG, "First visit for customer: " + customerId + ", starting with empty order");

                    // Always start with empty order items for a new customer
                    // This fixes the issue where items from one customer were showing for another
                    order.setTotal(0.0);
                    repo.setCurrentOrder(order);
                    repo.setCurrentOrderItems(new ArrayList<>());
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
            } catch (Exception e) {
                Log.e(TAG, "Error while setting customer ID: " + e.getMessage(), e);
            }
        } else {
            Log.w(TAG, "Cannot set customer ID, current order is null");
        }
    }

    /**
     * Set the customer ID for the current order
     * Overloaded method for backward compatibility, defaults to not preserving
     * items
     */
    public void setCustomerId(long customerId) {
        setCustomerId(customerId, false);
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