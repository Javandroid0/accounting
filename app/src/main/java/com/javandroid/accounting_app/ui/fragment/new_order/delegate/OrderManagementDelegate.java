package com.javandroid.accounting_app.ui.fragment.new_order.delegate;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
// Import the common listener
import com.javandroid.accounting_app.ui.adapter.order.OrderItemInteractionListener;
import com.javandroid.accounting_app.MainActivity;
import com.javandroid.accounting_app.data.model.CustomerEntity;
import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.model.ProductEntity;
import com.javandroid.accounting_app.data.model.UserEntity;
// The delegate no longer needs a direct adapter reference if listener handles all ops
// import com.javandroid.accounting_app.ui.adapter.order_editor.OrderEditorAdapter; // Or ScanOrderAdapter
import com.javandroid.accounting_app.ui.viewmodel.new_order.CurrentOrderViewModel;
import com.javandroid.accounting_app.ui.viewmodel.customer.CustomerOrderStateViewModel;
import com.javandroid.accounting_app.ui.viewmodel.product.ProductViewModel;
import com.google.android.material.button.MaterialButton;


import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Implement the common interface
public class OrderManagementDelegate implements OrderItemInteractionListener {
    private static final String TAG = "OrderManagementDelegate";

    private final Fragment fragment;
    private final CurrentOrderViewModel currentOrderViewModel;
    private final CustomerOrderStateViewModel customerOrderStateViewModel;
    private final ProductViewModel productViewModel;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private CustomerEntity selectedCustomer;
    private UserEntity currentUser;
    // No longer need direct adapter reference here if interaction is through listener methods
    // private ListAdapter<OrderItemEntity, ?> adapter; // Generic ListAdapter

    public OrderManagementDelegate(Fragment fragment,
                                   CurrentOrderViewModel currentOrderViewModel,
                                   CustomerOrderStateViewModel customerOrderStateViewModel,
                                   ProductViewModel productViewModel) {
        this.fragment = fragment;
        this.currentOrderViewModel = currentOrderViewModel;
        this.customerOrderStateViewModel = customerOrderStateViewModel;
        this.productViewModel = productViewModel;
    }

    // setAdapter might not be needed if delegate doesn't directly call adapter.submitList()
    // If CurrentOrderViewModel's LiveData drives the adapter, this can be removed.
    // public void setAdapter(ListAdapter<OrderItemEntity, ?> adapter) {
    //     this.adapter = adapter;
    // }


    public void setCustomer(CustomerEntity customer) {
        this.selectedCustomer = customer;
    }

    public void setUser(UserEntity user) {
        this.currentUser = user;
    }

    public void setupOrderObservers() {
        currentOrderViewModel.getFragmentOrderLiveData().observe(fragment.getViewLifecycleOwner(), order -> {
            updateTotalDisplay();
            // Adapter is updated by LiveData from CurrentOrderViewModel directly observed by Fragment
        });

        currentOrderViewModel.getFragmentOrderItemsLiveData().observe(fragment.getViewLifecycleOwner(), items -> {
            // Adapter is updated by LiveData from CurrentOrderViewModel directly observed by Fragment
            // if (adapter != null && items != null) {
            //     adapter.submitList(new ArrayList<>(items));
            // }
        });
    }

    public void updateTotalDisplay() {
        if (fragment.getView() != null) {
            OrderEntity currentOrder = currentOrderViewModel.getFragmentOrderLiveData().getValue();
            if (currentOrder != null) {
                MaterialButton btnConfirmOrder = fragment.getView().findViewById(
                        com.javandroid.accounting_app.R.id.btnConfirmOrder);
                if (btnConfirmOrder != null) {
                    btnConfirmOrder.setText(String.format(Locale.US, "تایید: %.3f", currentOrder.getTotal()));
                }
            }
        }
    }

    public void confirmOrder() {
        // ... (confirmOrder logic remains largely the same as before)
        // It uses currentOrderViewModel, selectedCustomer, currentUser.
        // This method was already quite detailed and correct in its flow.
        // Ensure CurrentOrderViewModel.confirmOrder() is robust.

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
        // ... (rest of the confirmOrder logic) ...
        // Ensure customer and user IDs are valid before calling currentOrderViewModel.confirmOrder()
        OrderEntity currentOrder = currentOrderViewModel.getFragmentOrderLiveData().getValue();
        List<OrderItemEntity> items = currentOrderViewModel.getFragmentOrderItemsLiveData().getValue();

        if (currentOrder != null) {
            // Ensure IDs are set on the order object if not already
            if (currentOrder.getCustomerId() <= 0 && selectedCustomer != null) {
                currentOrder.setCustomerId(selectedCustomer.getCustomerId());
            }
            if (currentOrder.getUserId() <= 0 && currentUser != null) {
                currentOrder.setUserId(currentUser.getUserId());
            }
            // The CurrentOrderViewModel should ideally handle setting these based on CustomerOrderStateViewModel

            // Call confirm on CurrentOrderViewModel
            currentOrderViewModel.confirmOrder();
        } else {
            Log.e(TAG, "Cannot confirm order: current order is null");
        }
    }

    public void prepareOrderForPrinting(Runnable onOrderReady) {
        // ... (prepareOrderForPrinting logic remains largely the same)
        // It ensures CurrentOrderViewModel has the right customer/user and then calls confirmOrderAndThen.
        OrderEntity currentOrder = currentOrderViewModel.getFragmentOrderLiveData().getValue();
        if (currentOrder != null && currentOrder.getOrderId() == 0) { // Not yet saved
            if (currentUser == null || selectedCustomer == null) {
                Toast.makeText(fragment.requireContext(), "Select user and customer first.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Ensure current order in ViewModel has the correct IDs before confirming for print
            if (currentOrder.getCustomerId() <= 0)
                currentOrder.setCustomerId(selectedCustomer.getCustomerId());
            if (currentOrder.getUserId() <= 0) currentOrder.setUserId(currentUser.getUserId());

            currentOrderViewModel.confirmOrderAndThen(onOrderReady);
        } else if (currentOrder != null) { // Already saved or no items
            onOrderReady.run();
        } else {
            Toast.makeText(fragment.requireContext(), "No order to print.", Toast.LENGTH_SHORT).show();
        }
    }


    // Implementation of OrderItemInteractionListener
    @Override
    public void onQuantityChanged(OrderItemEntity item, double newQuantity) {
        // This logic involves checking stock and updating ProductViewModel,
        // then updating CurrentOrderViewModel.
        // This was the complex part from the original OrderManagementDelegate.
        // It should remain largely similar.

        double currentQuantityInOrder = 0;
        List<OrderItemEntity> currentItems = currentOrderViewModel.getFragmentOrderItemsLiveData().getValue();
        if (currentItems != null) {
            for (OrderItemEntity oi : currentItems) {
                if (oi.getItemId() == item.getItemId()) {
                    currentQuantityInOrder = oi.getQuantity();
                    break;
                }
            }
        }

        final double quantityDifference = newQuantity - currentQuantityInOrder;

        Log.d(TAG, "Quantity change requested for product " + item.getProductId() + " (" + item.getProductName() +
                "): currentInOrder=" + currentQuantityInOrder + ", new=" + newQuantity + ", diff=" + quantityDifference);

        if (item.getProductId() == null || item.getProductId() <= 0) {
            Log.w(TAG, "Product ID is null or invalid for stock check. Updating quantity directly.");
            currentOrderViewModel.updateQuantity(item, newQuantity);
            return;
        }

        executor.execute(() -> {
            ProductEntity product = productViewModel.getProductByIdSync(item.getProductId());
            if (product == null) {
                Log.e(TAG, "Product not found for stock adjustment: ID " + item.getProductId());
                mainHandler.post(() -> {
                    Toast.makeText(fragment.requireContext(), "Product details not found for inventory check.", Toast.LENGTH_SHORT).show();
                    // Still update quantity in order if product not found for stock, as it might be a non-stocked item
                    currentOrderViewModel.updateQuantity(item, newQuantity);
                });
                return;
            }

            boolean stockSufficientOrNotApplicable = true;
            if (quantityDifference > 0) { // Increasing quantity in order, need to decrease stock
                if (product.getStock() < quantityDifference) {
                    stockSufficientOrNotApplicable = false;
                    mainHandler.post(() -> Toast.makeText(fragment.requireContext(),
                            "Not enough stock for " + product.getName() + ". Available: " + product.getStock(),
                            Toast.LENGTH_LONG).show());
                }
            }

            if (stockSufficientOrNotApplicable) {
                // Adjust stock: subtract difference if positive, add if negative
                double originalStock = product.getStock();
                product.setStock(originalStock - quantityDifference);
                productViewModel.update(product); // Update product stock in DB

                Log.d(TAG, "Stock updated for " + product.getName() + ": from " + originalStock + " to " + product.getStock());
                mainHandler.post(() -> currentOrderViewModel.updateQuantity(item, newQuantity));
            } else {
                // If stock was insufficient, the toast is already shown.
                // The UI (adapter) should ideally reflect this by not changing or reverting the quantity.
                // This requires the fragment to observe an event or for the adapter to handle it.
                // For now, the updateQuantity call won't happen.
                Log.w(TAG, "Stock insufficient, quantity change for item " + item.getProductName() + " not fully applied to order.");
                // To revert the visual change in adapter if it was optimistic:
                // mainHandler.post(() -> adapter.notifyItemChanged(position_of_item));
                // This is tricky without direct adapter access here. LiveData should refresh it.
            }
        });
    }

    @Override
    public void onPriceChanged(OrderItemEntity item, double newPrice) {
        // ScanOrder context might not allow price changes, or it might.
        // If it does, update CurrentOrderViewModel.
        // currentOrderViewModel.updatePrice(item, newPrice); // Hypothetical method
        Log.d(TAG, "Price change for " + item.getProductName() + " to " + newPrice + " (not implemented in CurrentOrderVM)");
        // For now, just ensure total is recalculated if price is considered part of item
        item.setSellPrice(newPrice); // Local update to the item passed in
        currentOrderViewModel.updateOrderTotal(); // Recalculates based on all items
    }

    @Override
    public void onDelete(OrderItemEntity item) {
        Log.d(TAG, "Deleting order item via delegate: " + item.getProductName());
        currentOrderViewModel.removeItem(item); // This should also handle total recalculation

        // Return quantity to stock
        if (item.getProductId() != null && item.getProductId() > 0 && item.getQuantity() > 0) {
            executor.execute(() -> {
                ProductEntity product = productViewModel.getProductByIdSync(item.getProductId());
                if (product != null) {
                    double originalStock = product.getStock();
                    product.setStock(originalStock + item.getQuantity());
                    productViewModel.update(product);
                    Log.d(TAG, "Stock restored for " + product.getName() + " by " + item.getQuantity() +
                            ": from " + originalStock + " to " + product.getStock());
                } else {
                    Log.e(TAG, "Product not found for stock restoration on delete: ID " + item.getProductId());
                }
            });
        }
    }

    public void onDestroy() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}