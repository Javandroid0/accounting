package com.javandroid.accounting_app.ui.fragment;

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
import java.util.ArrayList;
import java.util.List;

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

        initViews(view);
        setupViewModels();
        setupRecyclerView();
        setupTabs();
        setupListeners(view);
        observeViewModels();
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
            List<OrderItemEntity> items = orderItemsAdapter.getCurrentList();
            if (items != null && !items.isEmpty()) {
                exportOrdersToCsv(items);
            } else {
                Toast.makeText(requireContext(), "No items to export", Toast.LENGTH_SHORT).show();
            }
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
        String filename = "orders_" + System.currentTimeMillis() + ".csv";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);

        try (FileWriter writer = new FileWriter(file)) {
            writer.append("Barcode,Name,Quantity,SellPrice,BuyPrice\n");

            for (OrderItemEntity item : items) {
                writer.append(item.getBarcode()).append(",");
                writer.append(item.getProductName()).append(",");
                writer.append(String.valueOf(item.getQuantity())).append(",");
                writer.append(String.valueOf(item.getSellPrice())).append(",");
                writer.append(String.valueOf(item.getBuyPrice())).append("\n");
            }

            writer.flush();
            Toast.makeText(requireContext(), "Exported to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Failed to export CSV", Toast.LENGTH_SHORT).show();
        }
    }
}
