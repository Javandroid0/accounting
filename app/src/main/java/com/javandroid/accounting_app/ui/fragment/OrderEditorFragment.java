package com.javandroid.accounting_app.ui.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.ui.adapter.OrderEditorAdapter;
import com.javandroid.accounting_app.ui.adapter.SavedOrdersAdapter;
import com.javandroid.accounting_app.ui.viewmodel.SavedOrdersViewModel;
import com.javandroid.accounting_app.ui.viewmodel.OrderEditViewModel;
import com.javandroid.accounting_app.ui.viewmodel.CurrentOrderViewModel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderEditorFragment extends Fragment implements OrderEditorAdapter.OnOrderItemChangeListener {

    private SavedOrdersViewModel savedOrdersViewModel;
    private OrderEditViewModel orderEditViewModel;
    private CurrentOrderViewModel currentOrderViewModel;
    private OrderEditorAdapter orderItemsAdapter;
    private SavedOrdersAdapter savedOrdersAdapter;
    private RecyclerView recyclerView;
    private TextView emptyStateTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check and request storage permissions
        checkStoragePermissions();

        initViews(view);
        setupViewModels();
        setupRecyclerView();
        setupTabs();
        setupListeners(view);
        observeViewModels();
    }

    private void checkStoragePermissions() {
        // On Android 10+ we can use the scoped storage and don't need explicit
        // permissions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                        1001);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(),
                        "Storage permission is needed to export data",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_orders);
        emptyStateTextView = view.findViewById(R.id.empty_state_text);
    }

    private void setupViewModels() {
        savedOrdersViewModel = new ViewModelProvider(requireActivity()).get(SavedOrdersViewModel.class);
        orderEditViewModel = new ViewModelProvider(requireActivity()).get(OrderEditViewModel.class);
        currentOrderViewModel = new ViewModelProvider(requireActivity()).get(CurrentOrderViewModel.class);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Initialize adapters
        orderItemsAdapter = new OrderEditorAdapter(this);
        savedOrdersAdapter = new SavedOrdersAdapter(order -> {
            // Handle order click - navigate to order details or show items
            Toast.makeText(requireContext(), "Order #" + order.getOrderId() + " selected", Toast.LENGTH_SHORT).show();
            showOrderItems(order.getOrderId());
        });

        // Set initial adapter
        recyclerView.setAdapter(savedOrdersAdapter);
    }

    private void setupTabs() {
        // Set up the recycler view with saved orders adapter
        recyclerView.setAdapter(savedOrdersAdapter);
        savedOrdersViewModel.getAllOrders().observe(getViewLifecycleOwner(), orders -> {
            if (orders != null) {
                savedOrdersAdapter.submitList(orders);
                updateEmptyState(orders.isEmpty());
            }
        });
    }

    private void setupListeners(View view) {
        Button exportCsvButton = view.findViewById(R.id.btn_export_csv);
        Button saveChangesButton = view.findViewById(R.id.btn_save_changes);
        EditText etSearch = view.findViewById(R.id.et_search);

        exportCsvButton.setOnClickListener(v -> exportOrdersToCSV());
        saveChangesButton.setOnClickListener(v -> saveChanges());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                savedOrdersAdapter.filter(s.toString());
            }
        });
    }

    private void observeViewModels() {
        // Load saved orders
        savedOrdersViewModel.getAllOrders().observe(getViewLifecycleOwner(), orders -> {
            if (orders != null) {
                savedOrdersAdapter.submitList(orders);
                updateEmptyState(orders.isEmpty());
            }
        });

        // Observe order empty events (when order is deleted)
        orderEditViewModel.getOrderEmptyEvent().observe(getViewLifecycleOwner(), isEmpty -> {
            if (Boolean.TRUE.equals(isEmpty)) {
                // Refresh the list of orders
                savedOrdersViewModel.getAllOrders();
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            emptyStateTextView.setText("No orders found");
        } else {
            emptyStateTextView.setVisibility(View.GONE);
        }
    }

    private void showOrderItems(long orderId) {
        // Navigate to a separate fragment to view order details
        OrderEntity orderToEdit = null;
        List<OrderEntity> currentOrders = savedOrdersAdapter.getCurrentList();
        for (OrderEntity order : currentOrders) {
            if (order.getOrderId() == orderId) {
                orderToEdit = order;
                break;
            }
        }

        if (orderToEdit != null) {
            // Set the order for editing
            orderEditViewModel.setEditingOrder(orderToEdit);

            // Navigate to the OrderDetailsFragment
            Bundle args = new Bundle();
            args.putLong("orderId", orderId);
            Navigation.findNavController(requireView()).navigate(
                    R.id.action_orderEditorFragment_to_orderDetailsFragment, args);
        }
    }

    @Override
    public void onQuantityChanged(OrderItemEntity item, double newQuantity) {
        currentOrderViewModel.updateQuantity(item, newQuantity);
    }

    @Override
    public void onPriceChanged(OrderItemEntity item, double newPrice) {
        // Update price logic if needed
    }

    @Override
    public void onDelete(OrderItemEntity item) {
        currentOrderViewModel.removeItem(item);
    }

    private void exportOrdersToCSV() {
        // Implementation for exporting orders to CSV
        // Create file in Download directory
        try {
            File documentsDir = getContext().getExternalFilesDir(null);
            if (documentsDir != null) {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                File exportFile = new File(documentsDir, "orders_" + timestamp + ".csv");

                try (FileWriter writer = new FileWriter(exportFile)) {
                    // Write CSV header
                    writer.append("Order ID,Date,Customer ID,Total\n");

                    // Write each order
                    List<OrderEntity> orders = savedOrdersAdapter.getCurrentList();
                    for (OrderEntity order : orders) {
                        writer.append(String.format(Locale.getDefault(), "%d,%s,%d,%.2f\n",
                                order.getOrderId(),
                                order.getDate(),
                                order.getCustomerId(),
                                order.getTotal()));
                    }
                }

                Toast.makeText(getContext(), "Orders exported to " + exportFile.getAbsolutePath(),
                        Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error exporting orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void saveChanges() {
        orderEditViewModel.saveOrderChanges();
        Toast.makeText(requireContext(), "Changes saved", Toast.LENGTH_SHORT).show();
    }
}
