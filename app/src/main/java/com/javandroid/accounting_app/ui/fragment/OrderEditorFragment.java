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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.ui.adapter.OrderEditorAdapter;
import com.javandroid.accounting_app.ui.viewmodel.OrderViewModel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OrderEditorFragment extends Fragment implements OrderEditorAdapter.OnOrderItemChangeListener {

    private OrderViewModel orderViewModel;
    private OrderEditorAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_orders);
        Button btnSaveChanges = view.findViewById(R.id.btn_save_changes);
        Button btnExport = view.findViewById(R.id.btn_export_csv);
        EditText etSearch = view.findViewById(R.id.et_search);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        orderViewModel = new ViewModelProvider(requireActivity()).get(OrderViewModel.class);
        adapter = new OrderEditorAdapter(this);
        recyclerView.setAdapter(adapter);

        // Load current order items
        orderViewModel.getCurrentOrderItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                adapter.submitList(new ArrayList<>(items));
            }
        });

        btnSaveChanges.setOnClickListener(v -> {
            OrderEntity currentOrder = orderViewModel.getCurrentOrder().getValue();
            if (currentOrder != null) {
                orderViewModel.confirmOrder();
                Toast.makeText(requireContext(), "Order saved successfully", Toast.LENGTH_SHORT).show();
            }
        });

        btnExport.setOnClickListener(v -> {
            List<OrderItemEntity> items = adapter.getCurrentList();
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
                adapter.filter(s.toString());
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
        orderViewModel.deleteOrderItem(item);
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
