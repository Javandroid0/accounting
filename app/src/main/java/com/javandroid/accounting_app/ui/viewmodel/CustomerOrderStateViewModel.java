package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.repository.OrderStateRepository;

import java.util.List;

/**
 * ViewModel responsible for managing customer-specific order state
 */
public class CustomerOrderStateViewModel extends AndroidViewModel {
    private static final String TAG = "CustomerOrderStateVM";

    private final OrderStateRepository stateRepository;

    public CustomerOrderStateViewModel(@NonNull Application application) {
        super(application);
        stateRepository = OrderStateRepository.getInstance();
    }

    /**
     * Set the customer ID for the current order
     * and load any saved state for that customer
     */
    public void setCustomerId(long customerId) {
        OrderEntity order = stateRepository.getCurrentOrderValue();
        if (order != null) {
            // Save current order state for the previous customer
            if (order.getCustomerId() > 0) {
                Log.d(TAG, "Saving state for previous customer: " + order.getCustomerId() +
                        " before switching to customer: " + customerId);
                saveCurrentOrderStateForCustomer(order.getCustomerId());
            }

            // Set new customer ID
            order.setCustomerId(customerId);
            stateRepository.setCurrentOrder(order);

            // If we already have an order for this customer, load it
            if (stateRepository.hasStoredOrderForCustomer(customerId)) {
                OrderEntity savedOrder = stateRepository.getStoredOrderForCustomer(customerId);
                Log.d(TAG, "Restoring cached order for customer: " + customerId +
                        ", cached total: " + (savedOrder != null ? savedOrder.getTotal() : "null"));

                // Update with saved data but keep the same order instance to avoid
                // breaking references in the UI
                if (savedOrder != null) {
                    order.setTotal(savedOrder.getTotal());
                    stateRepository.setCurrentOrder(order);
                }

                // Load this customer's items
                List<OrderItemEntity> savedItems = stateRepository.getStoredOrderItemsForCustomer(customerId);
                if (savedItems != null) {
                    Log.d(TAG, "Restoring " + savedItems.size() + " cached items for customer: " + customerId);
                    stateRepository.setCurrentOrderItems(savedItems);
                } else {
                    Log.d(TAG, "No cached items for customer: " + customerId + ", using empty list");
                    stateRepository.setCurrentOrderItems(new java.util.ArrayList<>());
                }
            } else {
                // First time seeing this customer, start with empty order
                Log.d(TAG, "First visit for customer: " + customerId + ", starting with empty order");
                order.setTotal(0.0);
                stateRepository.setCurrentOrderItems(new java.util.ArrayList<>());
            }
        } else {
            Log.w(TAG, "Cannot set customer ID, current order is null");
        }
    }

    /**
     * Save the current order state for a customer
     */
    private void saveCurrentOrderStateForCustomer(long customerId) {
        OrderEntity currentOrderValue = stateRepository.getCurrentOrderValue();
        List<OrderItemEntity> currentItemsValue = stateRepository.getCurrentOrderItemsValue();

        if (currentOrderValue != null) {
            Log.d(TAG, "Caching order for customer: " + customerId +
                    ", total: " + currentOrderValue.getTotal());
        }

        if (currentItemsValue != null) {
            Log.d(TAG, "Caching " + currentItemsValue.size() + " items for customer: " + customerId);
        } else {
            Log.d(TAG, "No items to cache for customer: " + customerId);
        }

        stateRepository.storeOrderStateForCustomer(customerId, currentOrderValue, currentItemsValue);
    }

    /**
     * Set the current user ID
     */
    public void setCurrentUserId(long userId) {
        stateRepository.setCurrentUserId(userId);
    }

    /**
     * Clear stored order state for a customer
     */
    public void clearCustomerOrderState(long customerId) {
        stateRepository.clearStoredOrderForCustomer(customerId);
    }

    /**
     * Clear all stored order states
     */
    public void clearAllCustomerOrderStates() {
        stateRepository.clearAllStoredOrders();
    }
}