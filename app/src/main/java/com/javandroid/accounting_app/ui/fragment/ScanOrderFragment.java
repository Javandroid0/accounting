package com.javandroid.accounting_app.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.client.android.Intents;
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.Order;
import com.javandroid.accounting_app.data.model.Product;
import com.javandroid.accounting_app.ui.adapter.OrderAdapter;
import com.javandroid.accounting_app.ui.viewmodel.OrderViewModel;
import com.javandroid.accounting_app.ui.viewmodel.ProductViewModel;
import com.journeyapps.barcodescanner.CaptureActivity;

public class ScanOrderFragment extends Fragment {

    private static final int REQUEST_CODE_SCAN = 1;

    private OrderViewModel orderViewModel;
    private ProductViewModel productViewModel;

    private OrderAdapter orderAdapter;

    public ScanOrderFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan_order, container, false);

        Button scanButton = view.findViewById(R.id.btn_scan_barcode);
        Button confirmButton = view.findViewById(R.id.btn_confirm_order);
        RecyclerView orderRecyclerView = view.findViewById(R.id.recycler_order_list);

        orderRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        orderAdapter = new OrderAdapter();
        orderRecyclerView.setAdapter(orderAdapter);

        // ViewModels
        orderViewModel = new ViewModelProvider(requireActivity()).get(OrderViewModel.class);
        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        // Observe current order list
        orderViewModel.getOrders().observe(getViewLifecycleOwner(), orders -> {
            orderAdapter.submitList(orders);
        });

        // Scan barcode
        scanButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CaptureActivity.class);
            startActivityForResult(intent, REQUEST_CODE_SCAN);
        });

        // Confirm order
        confirmButton.setOnClickListener(v -> {
            orderViewModel.confirmOrder();
            Toast.makeText(getContext(), "Order Confirmed!", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SCAN && resultCode == Activity.RESULT_OK && data != null) {
            String barcode = data.getStringExtra(Intents.Scan.RESULT);

            if (barcode != null && !barcode.isEmpty()) {
                checkAndAddProduct(barcode);
            }
        }
    }

    private void checkAndAddProduct(String barcode) {
        // Get product by barcode from repository (or DB query)
        Product product = productViewModel.getProductByBarcode(barcode); // You must implement this

        if (product != null) {
            // Create Order from product
            Order order = new Order();
            order.setProductId(product.getBarcode());
            order.setProductName(product.getName());
            order.setProductSellPrice(product.getSellPrice());
            order.setProductBuyPrice(product.getBuyPrice());
            order.setQuantity(1);
//            product.getId(), product.getName(), product.getSellPrice(), 1
            orderViewModel.addProductToOrder(order);
        } else {
            // Show dialog or navigate to AddProductFragment
            new AlertDialog.Builder(getContext())
                    .setTitle("Product not found")
                    .setMessage("Do you want to add this product?")
                    .setPositiveButton("Add", (dialog, which) -> {
                        // Navigate to add product screen
                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_scanOrderFragment_to_addProductFragment);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }
}
