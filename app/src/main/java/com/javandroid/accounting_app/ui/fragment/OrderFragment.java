package com.javandroid.accounting_app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.Order;
import com.javandroid.accounting_app.ui.adapter.ProductAdapter;
import com.javandroid.accounting_app.ui.viewmodel.OrderViewModel;

import java.util.ArrayList;
import java.util.List;

public class OrderFragment extends Fragment {

    private OrderViewModel orderViewModel;
    private RecyclerView orderRecyclerView;
    private ProductAdapter productAdapter;
    private TextView totalAmountTextView;
    private Button confirmOrderButton;

    public OrderFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_order, container, false);

        // Initialize OrderViewModel
        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);

        // Set up the RecyclerView and Adapter
        orderRecyclerView = rootView.findViewById(R.id.recyclerViewOrderList);
        orderRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapter with onItemClickListener to handle product removal
        productAdapter = new ProductAdapter(new ArrayList<>(), order -> {
            // Handle remove product from the order
            orderViewModel.removeProductFromOrder(order);
        });
        orderRecyclerView.setAdapter(productAdapter);

        // Initialize UI elements
        totalAmountTextView = rootView.findViewById(R.id.totalAmount);
        confirmOrderButton = rootView.findViewById(R.id.confirmOrderButton);

        // Observe LiveData for order list updates
        orderViewModel.getOrders().observe(getViewLifecycleOwner(), this::updateOrderList);

        // Handle order confirmation
        confirmOrderButton.setOnClickListener(v -> confirmOrder());

        return rootView;
    }

    // Method to update the RecyclerView with the latest order list
    private void updateOrderList(List<Order> orders) {
        if (orders != null) {
            productAdapter.updateOrderList(orders);
            updateTotalAmount(orders);
        }
    }

    // Method to calculate and update the total amount of the current order
    private void updateTotalAmount(List<Order> orders) {
        double totalAmount = 0.0;
        for (Order order : orders) {
            totalAmount += order.getQuantity() * order.getProductSellPrice();
        }
        totalAmountTextView.setText(String.format("Total: $%.2f", totalAmount));
    }

    // Method to handle confirming the order
    private void confirmOrder() {
        orderViewModel.confirmOrder();
        // Navigate to the next screen or show confirmation dialog
    }
}
