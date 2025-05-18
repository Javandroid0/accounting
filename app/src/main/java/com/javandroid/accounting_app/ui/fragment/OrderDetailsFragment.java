package com.javandroid.accounting_app.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.ui.adapter.OrderEditorAdapter;
import com.javandroid.accounting_app.ui.viewmodel.OrderViewModel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.List;

public class OrderDetailsFragment extends Fragment implements OrderEditorAdapter.OnOrderItemChangeListener {

    private static final String TAG = "OrderDetailsFragment";
    private OrderViewModel orderViewModel;
    private OrderEditorAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvOrderId;
    private TextView tvOrderDate;
    private TextView tvOrderTotal;
    private Button btnSave;
    private Button btnCancel;
    private long orderId;
    private boolean hasChanges = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get order ID from arguments using SafeArgs
        if (getArguments() != null) {
            orderId = getArguments().getLong("orderId");
            Log.d(TAG, "DEBUG: Fragment created for order ID: " + orderId);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "DEBUG: Fragment view created for order ID: " + orderId);

        initViews(view);
        setupViewModel();
        setupRecyclerView();
        setupButtonListeners();
        loadOrderData();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "DEBUG: Fragment started for order ID: " + orderId);
    }

    private void initViews(View view) {
        tvOrderId = view.findViewById(R.id.tv_order_id);
        tvOrderDate = view.findViewById(R.id.tv_order_date);
        tvOrderTotal = view.findViewById(R.id.tv_order_total);
        recyclerView = view.findViewById(R.id.recycler_order_items);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);
    }

    private void setupViewModel() {
        orderViewModel = new ViewModelProvider(requireActivity()).get(OrderViewModel.class);
        Log.d(TAG, "DEBUG: ViewModel setup complete");
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OrderEditorAdapter(this);
        recyclerView.setAdapter(adapter);
        Log.d(TAG, "DEBUG: RecyclerView setup complete");
    }

    private void setupButtonListeners() {
        btnSave.setOnClickListener(v -> saveChanges());
        btnCancel.setOnClickListener(v -> cancelEditing());
        Log.d(TAG, "DEBUG: Button listeners setup complete");
    }

    private void loadOrderData() {
        Log.d(TAG, "DEBUG: Loading data for order ID: " + orderId);

        // Get the order from database and set it for editing (only once)
        orderViewModel.getOrderById(orderId).observe(getViewLifecycleOwner(), order -> {
            if (order != null) {
                Log.d(TAG, "DEBUG: Order loaded - ID: " + order.getOrderId() +
                        ", date: " + order.getDate() +
                        ", total: " + order.getTotal() +
                        ", customer: " + order.getCustomerId());

                // Set as current editing order (this will also load items)
                orderViewModel.setEditingOrder(order);

                // Observe the current order for UI updates
                orderViewModel.getCurrentOrder().observe(getViewLifecycleOwner(), this::updateOrderDetails);

                // Observe the current order items for the adapter
                orderViewModel.getCurrentOrderItems().observe(getViewLifecycleOwner(), items -> {
                    if (items != null) {
                        Log.d(TAG, "DEBUG: Items loaded - count: " + items.size() + " for order ID: " + orderId);
                        // Log each item
                        for (int i = 0; i < Math.min(items.size(), 5); i++) {
                            OrderItemEntity item = items.get(i);
                            Log.d(TAG, "DEBUG: Item " + i + ": " + item.getProductName() +
                                    ", quantity: " + item.getQuantity() +
                                    ", price: " + item.getSellPrice());
                        }
                        if (items.size() > 5) {
                            Log.d(TAG, "DEBUG: ... and " + (items.size() - 5) + " more items");
                        }

                        adapter.submitList(items);
                    } else {
                        Log.d(TAG, "DEBUG: No items found for order ID: " + orderId);
                        adapter.submitList(new ArrayList<>());
                    }
                });
            } else {
                Log.w(TAG, "DEBUG: Failed to load order: " + orderId);
            }
        });
    }

    private void updateOrderDetails(OrderEntity order) {
        tvOrderId.setText(getString(R.string.order_id_format, order.getOrderId()));
        tvOrderDate.setText(order.getDate());

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        tvOrderTotal.setText(currencyFormat.format(order.getTotal()));
    }

    private void saveChanges() {
        Log.d(TAG, "DEBUG: Save button clicked, hasChanges: " + hasChanges);

        if (hasChanges) {
            // Dump current state before saving
            OrderEntity order = orderViewModel.getCurrentOrder().getValue();
            List<OrderItemEntity> items = orderViewModel.getCurrentOrderItems().getValue();

            Log.d(TAG, "DEBUG: About to save - order: " + (order != null ? order.getOrderId() : "null") +
                    ", total: " + (order != null ? order.getTotal() : "null") +
                    ", items: " + (items != null ? items.size() : 0));

            // Use the new method to save all changes explicitly
            orderViewModel.saveOrderChanges();
            Toast.makeText(requireContext(), "Changes saved", Toast.LENGTH_SHORT).show();

            // Clean up observers after saving changes, just like we do when canceling
            clearCurrentOrderData();
        } else {
            Toast.makeText(requireContext(), "No changes to save", Toast.LENGTH_SHORT).show();
        }

        Log.d(TAG, "DEBUG: Navigating up after save");
        // Return to the previous screen
        Navigation.findNavController(requireView()).navigateUp();
    }

    private void cancelEditing() {
        Log.d(TAG, "DEBUG: Cancel button clicked, hasChanges: " + hasChanges);

        OrderEntity order = orderViewModel.getCurrentOrder().getValue();

        // Ask user for confirmation before deleting
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Order")
                .setMessage("Do you want to delete this order from the database?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (order != null && order.getOrderId() > 0) {
                        // Delete the order from the database
                        orderViewModel.deleteOrderAndItems(order.getOrderId());
                        Toast.makeText(requireContext(), "Order deleted", Toast.LENGTH_SHORT).show();
                    } else {
                        // If it's a new order, just discard changes
                        orderViewModel.cancelOrderEditing();
                        Toast.makeText(requireContext(), "Changes discarded", Toast.LENGTH_SHORT).show();
                    }

                    // Aggressively clean up all references
                    clearCurrentOrderData();

                    // Return to the previous screen
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Just discard changes without deleting
                    orderViewModel.cancelOrderEditing();
                    Toast.makeText(requireContext(), "Changes cancelled", Toast.LENGTH_SHORT).show();

                    // Aggressively clean up all references
                    clearCurrentOrderData();

                    // Return to the previous screen
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .setCancelable(true)
                .show();
    }

    @Override
    public void onQuantityChanged(OrderItemEntity item, double newQuantity) {
        // Update the quantity but don't save to database yet
        Log.d(TAG, "DEBUG: Quantity changed for item: " + item.getProductName() +
                ", from: " + item.getQuantity() + " to: " + newQuantity);
        orderViewModel.updateQuantity(item, newQuantity);
        hasChanges = true;
    }

    @Override
    public void onPriceChanged(OrderItemEntity item, double newPrice) {
        // Update the price in memory only, don't save to database yet
        Log.d(TAG, "DEBUG: Price changed for item: " + item.getProductName() +
                ", from: " + item.getSellPrice() + " to: " + newPrice);
        item.setSellPrice(newPrice);
        // Do not call updateOrderItem directly anymore
        hasChanges = true;

        // Update UI to reflect the price change
        OrderEntity order = orderViewModel.getCurrentOrder().getValue();
        if (order != null) {
            orderViewModel.updateOrderTotal();
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
            tvOrderTotal.setText(currencyFormat.format(order.getTotal()));
        }
    }

    @Override
    public void onDelete(OrderItemEntity item) {
        // We will not immediately remove from database
        // Only mark it as removed in the UI
        Log.d(TAG, "DEBUG: Item deleted: " + item.getProductName() +
                ", quantity: " + item.getQuantity() +
                ", price: " + item.getSellPrice());
        orderViewModel.removeItem(item);
        Toast.makeText(requireContext(), "Item removed", Toast.LENGTH_SHORT).show();
        hasChanges = true;

        // Update UI to reflect the total change
        OrderEntity order = orderViewModel.getCurrentOrder().getValue();
        if (order != null) {
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
            tvOrderTotal.setText(currencyFormat.format(order.getTotal()));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "DEBUG: Fragment stopped for order ID: " + orderId);
        // Ensure we clean up even if the user navigates away without using buttons
        clearCurrentOrderData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "DEBUG: Fragment destroyed for order ID: " + orderId);
    }

    private void clearCurrentOrderData() {
        // Reset the current order to a new empty order using the ViewModel method
        Log.d(TAG, "DEBUG: Clearing current order data in fragment");
        orderViewModel.resetCurrentOrder();

        // Clean up the observers for this order
        orderViewModel.cleanupOrderObservers(orderId);

        // Also reset the adapter to clear any cached items
        if (adapter != null) {
            adapter.submitList(null);
            Log.d(TAG, "DEBUG: Adapter list cleared");
        }
    }
}