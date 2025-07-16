package com.javandroid.accounting_app.ui.fragment.order;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.ui.adapter.order.OrderEditorAdapter;
import com.javandroid.accounting_app.ui.adapter.order.OrderItemInteractionListener; // Import the common listener
import com.javandroid.accounting_app.ui.viewmodel.order.OrderEditingManager;
import com.javandroid.accounting_app.ui.viewmodel.order.SavedOrdersViewModel;
import com.javandroid.accounting_app.ui.viewmodel.order.OrderEditViewModel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderDetailsFragment extends Fragment implements OrderItemInteractionListener { // Implement common listener

    private static final String TAG = "OrderDetailsFragment";

    private SavedOrdersViewModel savedOrdersViewModel;
    private OrderEditViewModel orderEditViewModelGlobal;
    private OrderEditingManager orderEditingManager;

    private Handler mainThreadHandler;

    private OrderEditorAdapter adapter;
    private RecyclerView recyclerViewOrderItems;
    private TextView tvOrderIdValue;
    private TextView tvOrderDateValue;
    private TextView tvOrderTotalValue;
    private Button btnToggleEditSave;
    private Button btnCancelOrDelete;
    private TextView tvPaymentStatus;

    private long orderIdArgs;
    private boolean isInEditMode = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            orderIdArgs = getArguments().getLong("orderId");
            Log.d(TAG, "Fragment created for order ID: " + orderIdArgs);
        }

        savedOrdersViewModel = new ViewModelProvider(requireActivity()).get(SavedOrdersViewModel.class);
        orderEditViewModelGlobal = new ViewModelProvider(requireActivity()).get(OrderEditViewModel.class);
        orderEditingManager = new OrderEditingManager();

        mainThreadHandler = new Handler(Looper.getMainLooper());
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
        Log.d(TAG, "View created for order ID: " + orderIdArgs);

        initViews(view);
        setupRecyclerView();
        setupButtonListeners();
        loadOrderDataAndObserve();
        updateUIForMode(); // Set initial UI state (read-only)
    }

    private void initViews(View view) {
        tvOrderIdValue = view.findViewById(R.id.tv_order_id);
        tvOrderDateValue = view.findViewById(R.id.tv_order_date);
        tvOrderTotalValue = view.findViewById(R.id.tv_order_total);
        recyclerViewOrderItems = view.findViewById(R.id.recycler_order_items);
        btnToggleEditSave = view.findViewById(R.id.btn_save); // XML ID for "Edit Order" / "Save Changes"
        btnCancelOrDelete = view.findViewById(R.id.btn_cancel); // XML ID for "Delete Order" / "Cancel Edit"
        tvPaymentStatus = view.findViewById(R.id.tv_payment_status);
    }


    private void setupRecyclerView() {
        recyclerViewOrderItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OrderEditorAdapter(this); // 'this' implements OrderItemInteractionListener
        recyclerViewOrderItems.setAdapter(adapter);
        Log.d(TAG, "RecyclerView setup complete");
    }

    private void updateAdapterEditableState() {
        if (adapter != null) {
            adapter.setEditable(isInEditMode); // Communicate edit mode to adapter
            Log.d(TAG, "Adapter edit mode set to: " + isInEditMode);
        }
    }

    private void setupButtonListeners() {
        btnToggleEditSave.setOnClickListener(v -> {
            if (isInEditMode) {
                saveChanges();
            } else {
                enterEditMode();
            }
        });

        btnCancelOrDelete.setOnClickListener(v -> {
            if (isInEditMode) {
                cancelEditMode();
            } else {
                confirmDeleteOrder();
            }
        });
    }

    private void loadOrderDataAndObserve() {
        Log.d(TAG, "Loading data for order ID: " + orderIdArgs);
        if (orderIdArgs <= 0) {
            Log.e(TAG, "Invalid orderIdArgs: " + orderIdArgs + ". Cannot load order.");
            Toast.makeText(getContext(), "Invalid Order ID specified.", Toast.LENGTH_LONG).show();
            if (getView() != null) Navigation.findNavController(getView()).navigateUp();
            return;
        }

        // Observe the OrderEntity
        savedOrdersViewModel.getOrderById(orderIdArgs).observe(getViewLifecycleOwner(), new Observer<OrderEntity>() {
            @Override
            public void onChanged(OrderEntity orderEntity) {
                // Important: Remove observer after first load to prevent loops if this LiveData updates due to save.
                savedOrdersViewModel.getOrderById(orderIdArgs).removeObserver(this);

                if (orderEntity != null) {
                    // Now fetch items. Use orderEditViewModelGlobal to access its public orderItemRepository.
                    // This assumes orderItemRepository.getOrderItems() returns LiveData.
                    LiveData<List<OrderItemEntity>> itemsLiveData = orderEditViewModelGlobal.orderItemRepository.getOrderItems(orderIdArgs);
                    itemsLiveData.observe(getViewLifecycleOwner(), new Observer<List<OrderItemEntity>>() {
                        @Override
                        public void onChanged(List<OrderItemEntity> items) {
                            // Remove this observer as well after loading items for the initial setup.
                            itemsLiveData.removeObserver(this);

                            if (items != null) {
                                Log.d(TAG, "Order and " + items.size() + " items loaded for order ID: " + orderIdArgs);
                                orderEditingManager.loadOrderForEditing(orderEntity, items);
                            } else {
                                Log.w(TAG, "Items list is null for order ID: " + orderIdArgs + ". Loading order with empty item list.");
                                orderEditingManager.loadOrderForEditing(orderEntity, new ArrayList<>());
                            }
                        }
                    });
                } else {
                    Log.w(TAG, "Order with ID: " + orderIdArgs + " not found (returned null).");
                    Toast.makeText(getContext(), "Order not found. It may have been deleted.", Toast.LENGTH_LONG).show();
                    if (getView() != null) Navigation.findNavController(getView()).navigateUp();
                }
            }
        });

        // Observe changes from the local OrderEditingManager
        orderEditingManager.getLiveEditableOrder().observe(getViewLifecycleOwner(), this::updateOrderUIDetails);
        orderEditingManager.getLiveEditableOrderItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                Log.d(TAG, "Updating adapter with " + items.size() + " items from OrderEditingManager");
                adapter.submitList(new ArrayList<>(items)); // Pass a new list for DiffUtil
            }
        });
    }

    private void updateOrderUIDetails(OrderEntity order) {
        if (order != null && tvOrderIdValue != null) { // Check if views are available
            Log.d(TAG, "Updating UI for Order ID: " + order.getOrderId() + ", Total: " + order.getTotal());
            tvOrderIdValue.setText(getString(R.string.order_id_format, order.getOrderId()));
            tvOrderDateValue.setText(order.getDate());
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
            tvOrderTotalValue.setText(currencyFormat.format(order.getTotal()));
//            System.out.println(order.isPaid());

            if (order.isPaid()) {
                tvPaymentStatus.setText("Paid");
                tvPaymentStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvPaymentStatus.setText("Unpaid");
                tvPaymentStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        }
    }

    private void enterEditMode() {
        isInEditMode = true;
        updateUIForMode();
        Toast.makeText(getContext(), "Edit mode enabled.", Toast.LENGTH_SHORT).show();
    }

    private void cancelEditMode() {
        OrderEntity snapshotOrder = orderEditingManager.getOriginalOrderSnapshot();
        List<OrderItemEntity> snapshotItems = orderEditingManager.getOriginalItemsSnapshot();

        if (snapshotOrder != null && snapshotItems != null) {
            orderEditingManager.loadOrderForEditing(snapshotOrder, snapshotItems); // Reload original state
        }
        isInEditMode = false;
        updateUIForMode();
        Toast.makeText(getContext(), "Edits cancelled.", Toast.LENGTH_SHORT).show();
    }

    private void updateUIForMode() {
        if (btnToggleEditSave == null || btnCancelOrDelete == null)
            return; // Views might be destroyed

        if (isInEditMode) {
            btnToggleEditSave.setText("Save Changes");
            btnCancelOrDelete.setText("Cancel Edit");
        } else {
            btnToggleEditSave.setText("Edit Order");
            btnCancelOrDelete.setText("Delete Order");
        }
        updateAdapterEditableState(); // Ensure adapter reflects the mode
    }

    private void saveChanges() {
        if (!isInEditMode) {
            Log.w(TAG, "SaveChanges called when not in edit mode.");
            return;
        }

        if (!orderEditingManager.hasChanges()) {
            Toast.makeText(requireContext(), "No changes to save.", Toast.LENGTH_SHORT).show();
            isInEditMode = false; // Exit edit mode
            updateUIForMode();
            return;
        }

        OrderEntity editedOrder = orderEditingManager.getCurrentEditableOrder();
        List<OrderItemEntity> editedItems = orderEditingManager.getCurrentEditableOrderItems();
        // Get the original items snapshot for accurate diffing in the ViewModel
        List<OrderItemEntity> originalDbItemsSnapshot = orderEditingManager.getOriginalItemsSnapshot();

        if (editedOrder == null || editedItems == null || originalDbItemsSnapshot == null) {
            Toast.makeText(requireContext(), "Error: Incomplete order data for saving.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Attempting to save Order ID: " + editedOrder.getOrderId());

        // Use the new method in OrderEditViewModel
        orderEditViewModelGlobal.saveModifiedOrderAndItems(
                editedOrder,
                editedItems,
                originalDbItemsSnapshot, // Pass this for the ViewModel to diff against
                () -> {
                    // This callback is ensured to be on the main thread by OrderEditViewModel
                    Toast.makeText(requireContext(), "Order #" + editedOrder.getOrderId() + " updated successfully!", Toast.LENGTH_SHORT).show();
                    isInEditMode = false;
                    updateUIForMode();
                    // Navigate up or refresh the view if staying
                    if (getView() != null) Navigation.findNavController(getView()).navigateUp();
                }
        );
    }

    private void confirmDeleteOrder() {
        final OrderEntity orderToDelete = orderEditingManager.getCurrentEditableOrder();
        if (orderToDelete == null || orderToDelete.getOrderId() <= 0) {
            Toast.makeText(getContext(), "No valid order selected for deletion.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Order")
                .setMessage("Are you sure you want to delete Order #" + orderToDelete.getOrderId() + "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Log.d(TAG, "Deleting Order ID: " + orderToDelete.getOrderId());
                    // Use the new method in SavedOrdersViewModel
                    savedOrdersViewModel.deleteOrderAndCascade(orderToDelete, () -> {
                        // This callback is ensured to be on the main thread by SavedOrdersViewModel
                        Toast.makeText(requireContext(), "Order #" + orderToDelete.getOrderId() + " deleted.", Toast.LENGTH_SHORT).show();
                        if (getView() != null) Navigation.findNavController(getView()).navigateUp();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Implementation of OrderItemInteractionListener
    @Override
    public void onQuantityChanged(OrderItemEntity item, double newQuantity) {
        if (isInEditMode && item != null) {
            Log.d(TAG, "UI: Quantity changed for item ID " + item.getItemId() + " to " + newQuantity);
            orderEditingManager.updateItemQuantity(item.getItemId(), newQuantity);
        }
    }

    @Override
    public void onPriceChanged(OrderItemEntity item, double newPrice) {
        if (isInEditMode && item != null) {
            Log.d(TAG, "UI: Price changed for item ID " + item.getItemId() + " to " + newPrice);
            // Assuming OrderEditingManager has updateItemPrice
            // orderEditingManager.updateItemPrice(item.getItemId(), newPrice);
            // For now, log and note that OrderEditingManager needs this method if price editing is desired.
            Toast.makeText(getContext(), "Price editing not fully implemented in manager.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDelete(OrderItemEntity item) {
        if (isInEditMode && item != null) {
            Log.d(TAG, "UI: Delete requested for item ID " + item.getItemId());
            orderEditingManager.removeItem(item.getItemId());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mainThreadHandler != null) {
            mainThreadHandler.removeCallbacksAndMessages(null);
        }
        // Nullify views if not using View Binding
        recyclerViewOrderItems = null;
        tvOrderIdValue = null;
        tvOrderDateValue = null;
        tvOrderTotalValue = null;
        btnToggleEditSave = null;
        btnCancelOrDelete = null;
        adapter = null; // Release adapter reference
        Log.d(TAG, "OrderDetailsFragment view destroyed for order ID: " + orderIdArgs);
    }
}