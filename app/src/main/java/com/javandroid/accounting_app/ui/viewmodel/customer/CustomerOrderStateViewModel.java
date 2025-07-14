package com.javandroid.accounting_app.ui.viewmodel.customer;

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
     * Gets the current state repository from the session manager.
     */
    private OrderStateRepository getStateRepository() {
        stateRepository = sessionManager.getCurrentRepository();
        return stateRepository;
    }

    /**
     * Sets the customer for the current order and handles state transitions.
     * This now acts as a coordinator for more specific private methods.
     *
     * @param customerId The customer ID to set.
     * @param preserveCurrentItems If true, current items are preserved.
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
        if (order == null) {
            Log.w(TAG, "Cannot set customer ID, current order is null");
            return;
        }

        // If the customer is not changing, there's nothing to do.
        if (order.getCustomerId() == customerId) {
            Log.d(TAG, "Customer ID " + customerId + " is already set. No state change needed.");
            return;
        }

        try {
            // 1. Save the state for the outgoing customer, if applicable.
            handleStateForPreviousCustomer(repo, order, customerId, preserveCurrentItems);

            // 2. Set the new customer ID on the order.
            order.setCustomerId(customerId);
            repo.setCurrentOrder(order);

            // 3. Load the state for the incoming customer.
            loadStateForNewCustomer(repo, order, customerId, preserveCurrentItems);

            // 4. Perform a final validation of the order total.
            reconcileOrderTotal(repo);

        } catch (Exception e) {
            Log.e(TAG, "Error while setting customer ID: " + e.getMessage(), e);
        }
    }

    /**
     * Saves the current order state for the previous customer before switching.
     */
    private void handleStateForPreviousCustomer(OrderStateRepository repo, OrderEntity order, long newCustomerId, boolean preserveCurrentItems) {
        if (!preserveCurrentItems && order.getCustomerId() > 0 && order.getCustomerId() != newCustomerId) {
            Log.d(TAG, "Saving state for previous customer: " + order.getCustomerId());
            saveCurrentOrderStateForCustomer(order.getCustomerId());
        }
    }

    /**
     * Determines whether to restore a cached order or start a new one for the customer.
     */
    private void loadStateForNewCustomer(OrderStateRepository repo, OrderEntity order, long customerId, boolean preserveCurrentItems) {
        if (preserveCurrentItems) {
            Log.d(TAG, "Preserving current items and total for customer: " + customerId);
            return; // Do nothing, keep current items.
        }

        if (repo.hasStoredOrderForCustomer(customerId)) {
            restoreCachedOrder(repo, order, customerId);
        } else {
            startNewOrder(repo, order);
        }
    }

    /**
     * Restores a previously cached order and its items for the given customer.
     */
    private void restoreCachedOrder(OrderStateRepository repo, OrderEntity order, long customerId) {
        OrderEntity savedOrder = repo.getStoredOrderForCustomer(customerId);
        List<OrderItemEntity> savedItems = repo.getStoredOrderItemsForCustomer(customerId);

        Log.d(TAG, "Restoring cached order for customer: " + customerId);

        if (savedOrder != null) {
            order.setTotal(savedOrder.getTotal());
            repo.setCurrentOrder(order);
        }
        repo.setCurrentOrderItems(savedItems != null ? savedItems : new ArrayList<>());
    }

    /**
     * Resets the order state for a new customer visit.
     */
    private void startNewOrder(OrderStateRepository repo, OrderEntity order) {
        Log.d(TAG, "First visit for customer: " + order.getCustomerId() + ", starting with empty order");
        order.setTotal(0.0);
        repo.setCurrentOrder(order);
        repo.setCurrentOrderItems(new ArrayList<>());
    }

    /**
     * Recalculates the order total based on its items and updates if inconsistent.
     */
    private void reconcileOrderTotal(OrderStateRepository repo) {
        List<OrderItemEntity> finalItems = repo.getCurrentOrderItemsValue();
        OrderEntity finalOrder = repo.getCurrentOrderValue();

        if (finalItems != null && !finalItems.isEmpty() && finalOrder != null) {
            double calculatedTotal = 0.0;
            for (OrderItemEntity item : finalItems) {
                calculatedTotal += item.getQuantity() * item.getSellPrice();
            }

            if (Math.abs(calculatedTotal - finalOrder.getTotal()) > 0.001) {
                Log.w(TAG, "Order total mismatch after state change. Current: " + finalOrder.getTotal()
                        + ", Calculated: " + calculatedTotal + ". Fixing total.");
                finalOrder.setTotal(calculatedTotal);
                repo.setCurrentOrder(finalOrder);
            }
        }
        Log.d(TAG, "Final state after customer change - customerId=" + finalOrder.getCustomerId()
                + ", items=" + (finalItems != null ? finalItems.size() : "null")
                + ", total=" + finalOrder.getTotal());
    }

    // Overloaded method for backward compatibility.
    public void setCustomerId(long customerId) {
        setCustomerId(customerId, false);
    }

    /**
     * Force-sets the items and total, bypassing normal state-switching logic.
     */
    public void forceSetItemsAndTotal(List<OrderItemEntity> items, double total) {
        Log.d(TAG, "Force setting " + (items != null ? items.size() : 0) + " items and total=" + total);
        OrderStateRepository repo = getStateRepository();
        OrderEntity order = repo.getCurrentOrderValue();

        if (order != null) {
            order.setTotal(total);
            repo.setCurrentOrder(order);
            repo.setCurrentOrderItems(items != null ? new ArrayList<>(items) : new ArrayList<>());
            Log.d(TAG, "Items and total force-set successfully");
        } else {
            Log.w(TAG, "Cannot force set items and total: order is null");
        }
    }

    /**
     * Saves the current order state to the repository's cache for a specific customer.
     */
    private void saveCurrentOrderStateForCustomer(long customerId) {
        OrderStateRepository repo = getStateRepository();
        OrderEntity currentOrderValue = repo.getCurrentOrderValue();
        List<OrderItemEntity> currentItemsValue = repo.getCurrentOrderItemsValue();

        Log.d(TAG, "Caching order state for customer: " + customerId);
        repo.storeOrderStateForCustomer(customerId, currentOrderValue, currentItemsValue);
    }

    /**
     * Set the current user ID in the repository.
     */
    public void setCurrentUserId(long userId) {
        getStateRepository().setCurrentUserId(userId);
    }

    /**
     * Clear stored order state for a customer.
     */
    public void clearCustomerOrderState(long customerId) {
        getStateRepository().clearStoredOrderForCustomer(customerId);
    }

    /**
     * Clear all stored order states.
     */
    public void clearAllCustomerOrderStates() {
        getStateRepository().clearAllStoredOrders();
    }
}