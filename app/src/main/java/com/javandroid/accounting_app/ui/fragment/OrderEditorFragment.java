package com.javandroid.accounting_app.ui.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.Order;
import com.javandroid.accounting_app.ui.adapter.OrderEditorAdapter;
import com.javandroid.accounting_app.ui.viewmodel.OrderViewModel;
//import com.javandroid.accounting_app.viewmodel.OrderViewModel;

import java.util.List;

public class OrderEditorFragment extends Fragment {

    private OrderViewModel orderViewModel;
    private OrderEditorAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_orders);
        Button btnSaveChanges = view.findViewById(R.id.btn_save_changes);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        orderViewModel = new ViewModelProvider(requireActivity()).get(OrderViewModel.class);

        adapter = new OrderEditorAdapter(new OrderEditorAdapter.OnOrderChangeListener() {
            @Override
            public void onQuantityChanged(Order order, double newQuantity) {
                order.setQuantity(newQuantity);
            }

            @Override
            public void onPriceChanged(Order order, double newPrice) {
                order.setProductSellPrice(newPrice);
            }

            @Override
            public void onDelete(Order order) {
                orderViewModel.deleteOrder2(order);
            }
        });

        recyclerView.setAdapter(adapter);

        // Load orders
        orderViewModel.getAllOrders().observe(getViewLifecycleOwner(), orders -> {
            adapter.submitList(orders);
        });

        btnSaveChanges.setOnClickListener(v -> {
            List<Order> updatedOrders = adapter.getCurrentOrders();
            orderViewModel.updateOrders(updatedOrders);
        });

        EditText etSearch = view.findViewById(R.id.et_search);
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
}
