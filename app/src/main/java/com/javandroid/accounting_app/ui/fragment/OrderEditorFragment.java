package com.javandroid.accounting_app.ui.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.ui.adapter.OrderEditorAdapter;
import com.javandroid.accounting_app.ui.adapter.SavedOrdersAdapter;
import com.javandroid.accounting_app.ui.viewmodel.OrderViewModel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderEditorFragment extends Fragment implements OrderEditorAdapter.OnOrderItemChangeListener {

    private OrderViewModel orderViewModel;
    private OrderEditorAdapter orderItemsAdapter;
    private SavedOrdersAdapter savedOrdersAdapter;
    private RecyclerView recyclerView;
    private TabLayout tabLayout;
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
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
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
        tabLayout = view.findViewById(R.id.tab_layout);
        emptyStateTextView = view.findViewById(R.id.empty_state_text);
    }

    private void setupViewModels() {
        orderViewModel = new ViewModelProvider(requireActivity()).get(OrderViewModel.class);
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
        recyclerView.setAdapter(orderItemsAdapter);
    }

    private void setupTabs() {
        // Add tabs
        if (tabLayout.getTabCount() == 0) {
            tabLayout.addTab(tabLayout.newTab().setText("Current Order"));
            tabLayout.addTab(tabLayout.newTab().setText("Saved Orders"));
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    // Current order
                    recyclerView.setAdapter(orderItemsAdapter);
                    orderViewModel.getCurrentOrderItems().observe(getViewLifecycleOwner(), items -> {
                        if (items != null) {
                            orderItemsAdapter.submitList(new ArrayList<>(items));
                            updateEmptyState(items.isEmpty());
                        }
                    });
                } else {
                    // Saved orders
                    recyclerView.setAdapter(savedOrdersAdapter);
                    orderViewModel.getAllOrders().observe(getViewLifecycleOwner(), orders -> {
                        if (orders != null) {
                            savedOrdersAdapter.submitList(orders);
                            updateEmptyState(orders.isEmpty());
                        }
                    });
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Not needed
            }
        });
    }

    private void setupListeners(View view) {
        Button btnSaveChanges = view.findViewById(R.id.btn_save_changes);
        Button btnExport = view.findViewById(R.id.btn_export_csv);
        EditText etSearch = view.findViewById(R.id.et_search);

        btnSaveChanges.setOnClickListener(v -> {
            OrderEntity currentOrder = orderViewModel.getCurrentOrder().getValue();
            if (currentOrder != null) {
                orderViewModel.confirmOrder();
                Toast.makeText(requireContext(), "Order saved successfully", Toast.LENGTH_SHORT).show();
                // Switch to saved orders tab
                tabLayout.selectTab(tabLayout.getTabAt(1));
            }
        });

        btnExport.setOnClickListener(v -> {
            List<OrderEntity> orders = savedOrdersAdapter.getCurrentList();
            if (orders != null && !orders.isEmpty()) {
                exportAllOrdersData(orders);
            } else {
                Toast.makeText(requireContext(), "No orders to export", Toast.LENGTH_SHORT).show();
            }
//            if (tabLayout.getSelectedTabPosition() == 0) {
//                // Current order tab
//                List<OrderItemEntity> items = orderItemsAdapter.getCurrentList();
//                if (items != null && !items.isEmpty()) {
//                    exportOrdersToCsv(items);
//                } else {
//                    Toast.makeText(requireContext(), "No items to export", Toast.LENGTH_SHORT).show();
//                }
//            } else {
//                // Saved orders tab
//
//            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (tabLayout.getSelectedTabPosition() == 0) {
                    orderItemsAdapter.filter(s.toString());
                } else {
                    savedOrdersAdapter.filter(s.toString());
                }
            }
        });
    }

    private void observeViewModels() {
        // Initial data load based on selected tab
        if (tabLayout.getSelectedTabPosition() == 0) {
            orderViewModel.getCurrentOrderItems().observe(getViewLifecycleOwner(), items -> {
                if (items != null) {
                    orderItemsAdapter.submitList(new ArrayList<>(items));
                    updateEmptyState(items.isEmpty());
                }
            });
        } else {
            orderViewModel.getAllOrders().observe(getViewLifecycleOwner(), orders -> {
                if (orders != null) {
                    savedOrdersAdapter.submitList(orders);
                    updateEmptyState(orders.isEmpty());
                }
            });
        }

        // Observe order empty events (when order is deleted)
        orderViewModel.getOrderEmptyEvent().observe(getViewLifecycleOwner(), isEmpty -> {
            if (Boolean.TRUE.equals(isEmpty)) {
                // Switch to saved orders tab when an order is deleted
                tabLayout.selectTab(tabLayout.getTabAt(1));
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            if (tabLayout.getSelectedTabPosition() == 0) {
                emptyStateTextView.setText("No items in current order");
            } else {
                emptyStateTextView.setText("No saved orders found");
            }
        } else {
            emptyStateTextView.setVisibility(View.GONE);
        }
    }

    private void showOrderItems(long orderId) {
        // First, load the order itself to get full context
        orderViewModel.getOrderById(orderId).observe(getViewLifecycleOwner(), order -> {
            if (order != null) {
                // Set this as the current order in the view model
                orderViewModel.setEditingOrder(order);

                // Now load the items for this order
                orderViewModel.getOrderItems(orderId).observe(getViewLifecycleOwner(), items -> {
                    if (items != null) {
                        // Set these items as the current order items
                        orderViewModel.replaceCurrentOrderItems(items);

                        // Switch to the first tab
                        tabLayout.selectTab(tabLayout.getTabAt(0));
                    }
                });
            }
        });
    }

    @Override
    public void onQuantityChanged(OrderItemEntity item, double newQuantity) {
        orderViewModel.updateQuantity(item, newQuantity);
    }

    @Override
    public void onPriceChanged(OrderItemEntity item, double newPrice) {
        item.setSellPrice(newPrice);
        orderViewModel.updateOrderItem(item);
    }

    @Override
    public void onDelete(OrderItemEntity item) {
        orderViewModel.removeItem(item);
    }

    private void exportOrdersToCsv(List<OrderItemEntity> items) {
        // Create timestamp for the filename
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String filename = "current_order_" + timestamp + ".csv";

        // Create file in the Downloads directory
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }

        File file = new File(downloadsDir, filename);

        try (FileWriter writer = new FileWriter(file)) {
            // Write headers
            writer.append("Item ID,Product ID,Product Name,Barcode,Quantity,Buy Price,Sell Price,Item Total\n");

            // Write each item
            for (OrderItemEntity item : items) {
                writer.append(String.valueOf(item.getItemId())).append(",");
                writer.append(item.getProductId() != null ? String.valueOf(item.getProductId()) : "").append(",");
                writer.append(escapeCsvField(item.getProductName())).append(",");
                writer.append(escapeCsvField(item.getBarcode())).append(",");
                writer.append(String.valueOf(item.getQuantity())).append(",");
                writer.append(String.valueOf(item.getBuyPrice())).append(",");
                writer.append(String.valueOf(item.getSellPrice())).append(",");
                writer.append(String.valueOf(item.getQuantity() * item.getSellPrice())).append("\n");
            }

            writer.flush();
            Toast.makeText(requireContext(),
                    "Current order exported to " + file.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(),
                    "Failed to export CSV: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void exportAllOrdersData(List<OrderEntity> orders) {
        // Create timestamp for the filename
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String filename = "orders_export_" + timestamp + ".csv";

        // Create file in the Downloads directory
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }

        File file = new File(downloadsDir, filename);

        try (FileWriter writer = new FileWriter(file)) {
            // Write headers for the CSV file
            writer.append("Order ID,Date,Customer ID,User ID,Total");
            writer.append(",Item ID,Product ID,Product Name,Barcode,Quantity,Buy Price,Sell Price,Item Total\n");

            // For each order, get its items and write them all
            for (OrderEntity order : orders) {
                long orderId = order.getOrderId();

                // We need to get the order items in a blocking way
                final List<OrderItemEntity>[] orderItems = new List[1];
                final boolean[] dataLoaded = new boolean[1];

                // Observe the order items
                orderViewModel.getOrderItems(orderId).observe(getViewLifecycleOwner(), items -> {
                    orderItems[0] = items;
                    dataLoaded[0] = true;
                });

                // Wait briefly for data to load (not ideal but simple approach)
                // In production code, you might use a more sophisticated approach
                int maxAttempts = 10;
                for (int i = 0; i < maxAttempts && !dataLoaded[0]; i++) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                // If we got the items, write them to the CSV
                if (orderItems[0] != null) {
                    for (OrderItemEntity item : orderItems[0]) {
                        // Order details
                        writer.append(String.valueOf(order.getOrderId())).append(",");
                        writer.append(order.getDate()).append(",");
                        writer.append(String.valueOf(order.getCustomerId())).append(",");
                        writer.append(String.valueOf(order.getUserId())).append(",");
                        writer.append(String.valueOf(order.getTotal())).append(",");

                        // Item details
                        writer.append(String.valueOf(item.getItemId())).append(",");
                        writer.append(item.getProductId() != null ? String.valueOf(item.getProductId()) : "")
                                .append(",");
                        writer.append(escapeCsvField(item.getProductName())).append(",");
                        writer.append(escapeCsvField(item.getBarcode())).append(",");
                        writer.append(String.valueOf(item.getQuantity())).append(",");
                        writer.append(String.valueOf(item.getBuyPrice())).append(",");
                        writer.append(String.valueOf(item.getSellPrice())).append(",");
                        writer.append(String.valueOf(item.getQuantity() * item.getSellPrice())).append("\n");
                    }
                } else {
                    // If we couldn't get the items, just write the order info with empty item
                    // fields
                    writer.append(String.valueOf(order.getOrderId())).append(",");
                    writer.append(order.getDate()).append(",");
                    writer.append(String.valueOf(order.getCustomerId())).append(",");
                    writer.append(String.valueOf(order.getUserId())).append(",");
                    writer.append(String.valueOf(order.getTotal())).append(",");
                    writer.append(",,,,,,\n");
                }
            }

            writer.flush();
            Toast.makeText(requireContext(),
                    "Orders exported to " + file.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(),
                    "Failed to export: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to properly escape CSV fields that might contain commas or
    // quotes
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }

        // If the field contains commas, quotes, or newlines, wrap it in quotes
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            // Replace any quotes with double quotes (CSV standard for escaping quotes)
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
