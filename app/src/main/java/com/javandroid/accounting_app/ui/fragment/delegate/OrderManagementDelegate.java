package com.javandroid.accounting_app.ui.fragment.delegate;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.javandroid.accounting_app.MainActivity;
import com.javandroid.accounting_app.data.model.CustomerEntity;
import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.model.ProductEntity;
import com.javandroid.accounting_app.data.model.UserEntity;
import com.javandroid.accounting_app.ui.adapter.OrderEditorAdapter;
import com.javandroid.accounting_app.ui.viewmodel.CurrentOrderViewModel;
import com.javandroid.accounting_app.ui.viewmodel.CustomerOrderStateViewModel;
import com.javandroid.accounting_app.ui.viewmodel.ProductViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Delegate class to handle order management operations
 * Helps to reduce complexity in ScanOrderFragment
 */
public class OrderManagementDelegate implements OrderEditorAdapter.OnOrderItemChangeListener {
    private static final String TAG = "OrderManagementDelegate";

    private final Fragment fragment;
    private final CurrentOrderViewModel currentOrderViewModel;
    private final CustomerOrderStateViewModel customerOrderStateViewModel;
    private final ProductViewModel productViewModel;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private CustomerEntity selectedCustomer;
    private UserEntity currentUser;
    private OrderEditorAdapter adapter;

    public OrderManagementDelegate(Fragment fragment,
            CurrentOrderViewModel currentOrderViewModel,
            CustomerOrderStateViewModel customerOrderStateViewModel,
            ProductViewModel productViewModel) {
        this.fragment = fragment;
        this.currentOrderViewModel = currentOrderViewModel;
        this.customerOrderStateViewModel = customerOrderStateViewModel;
        this.productViewModel = productViewModel;
    }

    /**
     * Set the adapter for order items
     */
    public void setAdapter(OrderEditorAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Set the customer for this order
     */
    public void setCustomer(CustomerEntity customer) {
        this.selectedCustomer = customer;
    }

    /**
     * Set the user for this order
     */
    public void setUser(UserEntity user) {
        this.currentUser = user;
    }

    /**
     * Setup observers for order related data
     */
    public void setupOrderObservers() {
        // Observe current order and items
        currentOrderViewModel.getCurrentOrder().observe(fragment.getViewLifecycleOwner(), order -> {
            updateTotalDisplay();

            // Update the adapter with current items
            List<OrderItemEntity> items = currentOrderViewModel.getCurrentOrderItems().getValue();
            if (items != null && adapter != null) {
                adapter.submitList(new ArrayList<>(items));
            }
        });

        currentOrderViewModel.getCurrentOrderItems().observe(fragment.getViewLifecycleOwner(), items -> {
            if (items != null && adapter != null) {
                adapter.submitList(new ArrayList<>(items));
            }
        });
    }

    /**
     * Update the total display on the confirm button
     */
    public void updateTotalDisplay() {
        if (fragment.getView() != null) {
            OrderEntity currentOrder = currentOrderViewModel.getCurrentOrder().getValue();
            if (currentOrder != null) {
                // Update the confirm button text
                MaterialButton btnConfirmOrder = fragment.getView().findViewById(
                        com.javandroid.accounting_app.R.id.btnConfirmOrder);
                updateConfirmButtonText(btnConfirmOrder, currentOrder.getTotal());
            }
        }
    }

    /**
     * Update the confirm button text to include the total amount
     */
    private void updateConfirmButtonText(MaterialButton button, double total) {
        if (button != null) {
            // Format total as integer if it's a whole number
            button.setText(String.format("تایید %.3f", total));
        }
    }

    /**
     * Confirm the current order
     */
    public void confirmOrder() {
        if (currentUser == null) {
            Log.w(TAG, "Cannot confirm order: no user selected");
            Toast.makeText(fragment.requireContext(), "Please select a user first", Toast.LENGTH_SHORT).show();
            if (fragment.getActivity() instanceof MainActivity) {
                ((MainActivity) fragment.getActivity()).openUserDrawer();
            }
            return;
        }

        if (selectedCustomer == null) {
            Log.w(TAG, "Cannot confirm order: no customer selected");
            Toast.makeText(fragment.requireContext(), "Please select a customer first", Toast.LENGTH_SHORT).show();
            if (fragment.getActivity() instanceof MainActivity) {
                ((MainActivity) fragment.getActivity()).openCustomerDrawer();
            }
            return;
        }

        // Ensure customer and user IDs are set before continuing
        if (selectedCustomer.getCustomerId() <= 0) {
            Log.e(TAG, "Invalid customer ID: " + selectedCustomer.getCustomerId() + " for customer "
                    + selectedCustomer.getName());
            Toast.makeText(fragment.requireContext(), "Invalid customer ID, please select another customer",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser.getUserId() <= 0) {
            Log.e(TAG, "Invalid user ID: " + currentUser.getUserId() + " for user " + currentUser.getUsername());
            Toast.makeText(fragment.requireContext(), "Invalid user ID, please select another user", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        OrderEntity currentOrder = currentOrderViewModel.getCurrentOrder().getValue();
        List<OrderItemEntity> items = currentOrderViewModel.getCurrentOrderItems().getValue();

        Log.d(TAG, "DEBUG: Confirming order - ID: " + (currentOrder != null ? currentOrder.getOrderId() : "null") +
                ", customerId: " + (currentOrder != null ? currentOrder.getCustomerId() : "null") +
                ", userId: " + (currentOrder != null ? currentOrder.getUserId() : "null") +
                ", items: " + (items != null ? items.size() : 0) +
                ", total: " + (currentOrder != null ? currentOrder.getTotal() : "null"));

        if (currentOrder != null) {
            // Log order items details
            if (items != null && !items.isEmpty()) {
                Log.d(TAG, "Order items breakdown:");
                double calculatedTotal = 0.0;
                for (int i = 0; i < items.size(); i++) {
                    OrderItemEntity item = items.get(i);
                    double itemTotal = item.getQuantity() * item.getSellPrice();
                    calculatedTotal += itemTotal;
                    Log.d(TAG, "  Item " + (i + 1) + ": " + item.getProductName() +
                            ", ID=" + item.getItemId() +
                            ", productId=" + item.getProductId() +
                            ", quantity=" + item.getQuantity() +
                            ", price=" + item.getSellPrice() +
                            ", subtotal=" + itemTotal);
                }

                // Ensure total is correct before confirming
                if (Math.abs(calculatedTotal - currentOrder.getTotal()) > 0.001) {
                    Log.w(TAG, "Order total mismatch. Current: " + currentOrder.getTotal() +
                            ", Calculated: " + calculatedTotal + ". Updating to correct value.");
                    currentOrder.setTotal(calculatedTotal);
                    currentOrderViewModel.updateOrderTotal();
                } else {
                    Log.d(TAG, "Order total verified: " + currentOrder.getTotal());
                }
            }

            // Make sure the current order has the latest total before proceeding
            currentOrderViewModel.updateOrderTotal();

            // Setting customer and user IDs is now handled by CustomerOrderStateViewModel
            Log.d(TAG, "Setting customerId=" + selectedCustomer.getCustomerId() + " (" + selectedCustomer.getName() +
                    ") and userId=" + currentUser.getUserId() + " (" + currentUser.getUsername() + ")");

            // Make sure the user ID is set first
            customerOrderStateViewModel.setCurrentUserId(currentUser.getUserId());

            // Make sure to update items if necessary after user ID change
            if (items != null && !items.isEmpty()) {
                // Force update of the current items in the repository to ensure they're not
                // lost
                // This fixes an issue where the repository would lose items during customer ID
                // change
                currentOrderViewModel.refreshItems(items);
                Log.d(TAG, "Forcing refresh of " + items.size() + " items before setting customer");

                // Directly force-set items and total in CustomerOrderStateViewModel
                customerOrderStateViewModel.forceSetItemsAndTotal(items, currentOrder.getTotal());
            }

            // Now set the customer ID after ensuring items are saved
            customerOrderStateViewModel.setCustomerId(selectedCustomer.getCustomerId());

            // Verify customer and user IDs are set on the order
            if (currentOrder.getCustomerId() <= 0 || currentOrder.getUserId() <= 0) {
                // If not set through the ViewModel, set them directly
                Log.d(TAG, "Setting customer/user IDs directly on order - old values: " +
                        "customerId=" + currentOrder.getCustomerId() + ", userId=" + currentOrder.getUserId());

                currentOrder.setCustomerId(selectedCustomer.getCustomerId());
                currentOrder.setUserId(currentUser.getUserId());

                // Update the order in the repository
                currentOrderViewModel.getCurrentOrder().getValue().setCustomerId(selectedCustomer.getCustomerId());
                currentOrderViewModel.getCurrentOrder().getValue().setUserId(currentUser.getUserId());

                Log.d(TAG, "New values set: customerId=" + currentOrder.getCustomerId() +
                        ", userId=" + currentOrder.getUserId());
            }

            // Recheck the order total and items after setting customer and user
            Log.d(TAG, "Order state before confirmation - total: " +
                    currentOrderViewModel.getCurrentOrder().getValue().getTotal() +
                    ", items: "
                    + (currentOrderViewModel.getCurrentOrderItems().getValue() != null
                            ? currentOrderViewModel.getCurrentOrderItems().getValue().size()
                            : 0));

            // Force set items and total one more time to ensure consistency
            if (items != null && !items.isEmpty()) {
                // Get the current updated order and its total
                OrderEntity updatedOrder = currentOrderViewModel.getCurrentOrder().getValue();
                double updatedTotal = 0.0;

                // Recalculate total from items
                for (OrderItemEntity item : items) {
                    updatedTotal += item.getQuantity() * item.getSellPrice();
                }

                Log.d(TAG, "Final check before confirmation: calculated total=" + updatedTotal);

                // Update the total if needed
                if (updatedOrder != null && Math.abs(updatedOrder.getTotal() - updatedTotal) > 0.001) {
                    Log.w(TAG, "Total still incorrect before confirmation. Setting to " + updatedTotal);
                    // Don't use setValue directly as it's protected
                    updatedOrder.setTotal(updatedTotal);

                    // Update the order directly in the ViewModel
                    currentOrderViewModel.updateOrderTotal();
                }
            }

            // Confirm order but stay on this screen
            Log.d(TAG, "Calling confirmOrder() on CurrentOrderViewModel");
            currentOrderViewModel.confirmOrder();
            Toast.makeText(fragment.requireContext(), "Order confirmed successfully", Toast.LENGTH_SHORT).show();

            // Ensure UI is refreshed by explicitly refreshing the adapter with empty list
            // This handles cases where LiveData observers don't trigger immediately
            if (adapter != null) {
                Log.d(TAG, "Refreshing adapter with empty list to clear UI");
                adapter.submitList(new ArrayList<>());
            }

            // Update the total display to show 0
            Log.d(TAG, "Updating total display to reflect new state");
            updateTotalDisplay();

            // Check state after confirmation
            OrderEntity newOrder = currentOrderViewModel.getCurrentOrder().getValue();
            List<OrderItemEntity> newItems = currentOrderViewModel.getCurrentOrderItems().getValue();

            Log.d(TAG, "DEBUG: After confirmation - new order ID: " +
                    (newOrder != null ? newOrder.getOrderId() : "null") +
                    ", customerId: " + (newOrder != null ? newOrder.getCustomerId() : "null") +
                    ", userId: " + (newOrder != null ? newOrder.getUserId() : "null") +
                    ", items: " + (newItems != null ? newItems.size() : 0) +
                    ", total: " + (newOrder != null ? newOrder.getTotal() : "null"));
        } else {
            Log.e(TAG, "Cannot confirm order: current order is null");
        }
    }

    /**
     * Print the order, confirming it first if needed
     */
    public void prepareOrderForPrinting(Runnable onOrderReady) {
        // First, check if the order is confirmed (has ID)
        OrderEntity currentOrder = currentOrderViewModel.getCurrentOrder().getValue();
        if (currentOrder != null && currentOrder.getOrderId() == 0) {
            Log.d(TAG, "Order needs to be confirmed before printing - prepareOrderForPrinting");

            // Order is not confirmed yet, confirm it first and then print
            if (currentUser == null) {
                Log.w(TAG, "Cannot prepare order for printing: no user selected");
                Toast.makeText(fragment.requireContext(), "Please select a user first", Toast.LENGTH_SHORT).show();
                if (fragment.getActivity() instanceof MainActivity) {
                    ((MainActivity) fragment.getActivity()).openUserDrawer();
                }
                return;
            }

            if (selectedCustomer == null) {
                Log.w(TAG, "Cannot prepare order for printing: no customer selected");
                Toast.makeText(fragment.requireContext(), "Please select a customer first", Toast.LENGTH_SHORT).show();
                if (fragment.getActivity() instanceof MainActivity) {
                    ((MainActivity) fragment.getActivity()).openCustomerDrawer();
                }
                return;
            }

            // Ensure customer and user IDs are set before continuing
            if (selectedCustomer.getCustomerId() <= 0) {
                Log.e(TAG, "Invalid customer ID: " + selectedCustomer.getCustomerId() + " for customer "
                        + selectedCustomer.getName());
                Toast.makeText(fragment.requireContext(), "Invalid customer ID, please select another customer",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUser.getUserId() <= 0) {
                Log.e(TAG, "Invalid user ID: " + currentUser.getUserId() + " for user " + currentUser.getUsername());
                Toast.makeText(fragment.requireContext(), "Invalid user ID, please select another user",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Set customer and user IDs
            Log.d(TAG, "Setting customerId=" + selectedCustomer.getCustomerId() + " (" + selectedCustomer.getName() +
                    ") and userId=" + currentUser.getUserId() + " (" + currentUser.getUsername() + ")");

            customerOrderStateViewModel.setCustomerId(selectedCustomer.getCustomerId());
            customerOrderStateViewModel.setCurrentUserId(currentUser.getUserId());

            // Verify customer and user IDs are set on the order
            if (currentOrder.getCustomerId() <= 0 || currentOrder.getUserId() <= 0) {
                // If not set through the ViewModel, set them directly
                Log.d(TAG, "Setting customer/user IDs directly on order - old values: " +
                        "customerId=" + currentOrder.getCustomerId() + ", userId=" + currentOrder.getUserId());

                currentOrder.setCustomerId(selectedCustomer.getCustomerId());
                currentOrder.setUserId(currentUser.getUserId());

                // Update the order in the repository
                currentOrderViewModel.getCurrentOrder().getValue().setCustomerId(selectedCustomer.getCustomerId());
                currentOrderViewModel.getCurrentOrder().getValue().setUserId(currentUser.getUserId());

                Log.d(TAG, "New values set: customerId=" + currentOrder.getCustomerId() +
                        ", userId=" + currentOrder.getUserId());
            }

            // Check and correct the total if needed
            List<OrderItemEntity> items = currentOrderViewModel.getCurrentOrderItems().getValue();
            if (items != null && !items.isEmpty()) {
                double calculatedTotal = 0.0;
                for (OrderItemEntity item : items) {
                    calculatedTotal += item.getQuantity() * item.getSellPrice();
                }

                if (Math.abs(calculatedTotal - currentOrder.getTotal()) > 0.001) {
                    Log.w(TAG, "Order total mismatch before printing. Current: " + currentOrder.getTotal() +
                            ", Calculated: " + calculatedTotal + ". Updating to correct value.");
                    currentOrder.setTotal(calculatedTotal);
                    currentOrderViewModel.updateOrderTotal();
                }
            }

            // Confirm order and then print
            Log.d(TAG, "Calling confirmOrderAndThen() to confirm order before printing");
            currentOrderViewModel.confirmOrderAndThen(onOrderReady);
        } else {
            // Order already has an ID, simply run the callback
            Log.d(TAG, "Order already confirmed (ID: " + (currentOrder != null ? currentOrder.getOrderId() : "null") +
                    "), proceeding to print");
            onOrderReady.run();
        }
    }

    @Override
    public void onQuantityChanged(OrderItemEntity item, double newQuantity) {
        // Get the current quantity to calculate difference
        double currentQuantity = item.getQuantity();
        double quantityDifference = newQuantity - currentQuantity;

        // If quantity is increasing, we need to decrease stock
        if (quantityDifference > 0 && item.getProductId() != null) {
            executor.execute(() -> {
                ProductEntity product = productViewModel.getProductByIdSync(item.getProductId());
                if (product != null) {
                    // Check if we have enough stock
                    if (product.getStock() >= quantityDifference) {
                        // Decrease stock by the difference
                        product.setStock(product.getStock() - quantityDifference);
                        productViewModel.updateProduct(product);

                        // Update quantity in the order item
                        mainHandler.post(() -> currentOrderViewModel.updateQuantity(item, newQuantity));
                    } else {
                        // Not enough stock
                        mainHandler.post(() -> {
                            Toast.makeText(fragment.requireContext(),
                                    "Not enough stock available. Only " + product.getStock() + " left.",
                                    Toast.LENGTH_SHORT).show();

                            // Reset quantity field to current value
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                } else {
                    // Product not found, but still update quantity
                    mainHandler.post(() -> currentOrderViewModel.updateQuantity(item, newQuantity));
                }
            });
        } else {
            // If quantity is decreasing, we need to add back to stock
            if (quantityDifference < 0 && item.getProductId() != null) {
                executor.execute(() -> {
                    ProductEntity product = productViewModel.getProductByIdSync(item.getProductId());
                    if (product != null) {
                        // Increase stock by the absolute difference
                        product.setStock(product.getStock() + Math.abs(quantityDifference));
                        productViewModel.updateProduct(product);
                    }

                    // Update quantity in the order item
                    mainHandler.post(() -> currentOrderViewModel.updateQuantity(item, newQuantity));
                });
            } else {
                // No change in quantity or no product ID
                currentOrderViewModel.updateQuantity(item, newQuantity);
            }
        }
    }

    @Override
    public void onPriceChanged(OrderItemEntity item, double newPrice) {
        // Update price logic if needed
    }

    @Override
    public void onDelete(OrderItemEntity item) {
        // Use the remove method in CurrentOrderViewModel
        currentOrderViewModel.removeItem(item);

        // If needed, return quantity to stock
        if (item.getProductId() != null) {
            executor.execute(() -> {
                ProductEntity product = productViewModel.getProductByIdSync(item.getProductId());
                if (product != null) {
                    // Add the quantity back to stock
                    product.setStock(product.getStock() + item.getQuantity());
                    productViewModel.updateProduct(product);
                }
            });
        }
    }

    /**
     * Clean up resources when the delegate is no longer needed
     */
    public void onDestroy() {
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }
}