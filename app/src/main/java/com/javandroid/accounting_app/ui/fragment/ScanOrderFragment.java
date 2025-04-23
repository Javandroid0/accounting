package com.javandroid.accounting_app.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
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

import java.util.List;

public class ScanOrderFragment extends Fragment {

    private static final int REQUEST_CODE_SCAN = 1;

    private OrderViewModel orderViewModel;
    private ProductViewModel productViewModel;

    private OrderAdapter orderAdapter;
    private TextView totalTextView;

    private LinearLayout orderContainer;


    private String currentUserId;

    public ScanOrderFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan_order, container, false);


        Button scanButton = view.findViewById(R.id.btn_scan_barcode);
        Button confirmButton = view.findViewById(R.id.btn_confirm_order);
       totalTextView = view.findViewById(R.id.tv_total_price); // ⚠️ You need to add this to your XML
        orderContainer = view.findViewById(R.id.order_container);



        orderAdapter = new OrderAdapter();

        orderViewModel = new ViewModelProvider(requireActivity()).get(OrderViewModel.class);
        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);

        orderViewModel.setCurrentUserId();
        currentUserId = orderViewModel.getCurrentUserId();


        orderViewModel.getCurrentOrderList().observe(getViewLifecycleOwner(), orders -> {
            orderContainer.removeAllViews(); // clear previous views
            for (Order order : orders) {
                addProductView(order);
            }
            updateTotal(orders);
        });

        scanButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CaptureActivity.class);
            startActivityForResult(intent, REQUEST_CODE_SCAN);
        });

        confirmButton.setOnClickListener(v -> {
            orderViewModel.confirmOrderForUser(currentUserId);
            Toast.makeText(getContext(), "Order Confirmed!", Toast.LENGTH_SHORT).show();

            currentUserId = orderViewModel.getCurrentUserId(); // 🔁 Update to new session ID


            currentUserId = orderViewModel.getCurrentUserId(); // new session ID
            orderContainer.removeAllViews(); // reset UI

            // Refresh the UI with new user's (empty) order list
//            orderViewModel.getCurrentOrderList().observe(getViewLifecycleOwner(), orders -> {
//                orderAdapter.submitList(orders);
//                updateTotal(orders);
//            });
        });
        EditText etManualBarcode = view.findViewById(R.id.et_manual_barcode);

        // Add listener for manual barcode input (e.g., press Enter or loss of focus)
        etManualBarcode.setOnEditorActionListener((v, actionId, event) -> {
            String barcode = etManualBarcode.getText().toString().trim();
            if (!barcode.isEmpty()) {
                checkAndAddProduct(barcode);
                etManualBarcode.setText(""); // clear input
                return true;
            }
            return false;
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
        productViewModel.getProductByBarcode(barcode).observe(getViewLifecycleOwner(), product -> {
            if (product != null) {
                Order order = new Order();
                order.setProductId(product.getId());
                order.setProductBarcode(product.getBarcode());
                order.setProductName(product.getName());
                order.setProductSellPrice(product.getSellPrice());
                order.setProductBuyPrice(product.getBuyPrice());
                order.setQuantity(1);
                order.setUserId(currentUserId); // ✅ Assign user ID

                orderViewModel.addProductToOrder(order);
            } else {
                new AlertDialog.Builder(getContext())
                        .setTitle("Product not found")
                        .setMessage("Do you want to add this product?")
                        .setPositiveButton("Add", (dialog, which) -> {
                            Bundle bundle = new Bundle();
                            bundle.putString("scanned_barcode", barcode);
                            Navigation.findNavController(requireView())
                                    .navigate(R.id.action_scanOrderFragment_to_addProductFragment, bundle);
//                            Navigation.findNavController(requireView())
//                                    .navigate(R.id.action_scanOrderFragment_to_addProductFragment);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    private void addProductView(Order order) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View itemView = inflater.inflate(R.layout.partial_order_item, orderContainer, false);

        TextView name = itemView.findViewById(R.id.tv_product_name);


        TextView emptySpace = itemView.findViewById(R.id.emptySpace);

        TextView price = itemView.findViewById(R.id.tv_product_price);
        TextView quantity = itemView.findViewById(R.id.tv_quantity);
        Button increase = itemView.findViewById(R.id.btn_increase);
        Button decrease = itemView.findViewById(R.id.btn_decrease);

        name.setText(order.getProductName());

        price.setText(String.valueOf(order.getProductSellPrice()));
        quantity.setText(String.valueOf(order.getQuantity()));

        increase.setOnClickListener(v -> {
            order.setQuantity(order.getQuantity() + 1);
//            orderViewModel.updateOrder(order);
            orderViewModel.updateOrderInMemory(order);
            quantity.setText(String.valueOf(order.getQuantity()));
            updateTotal(orderViewModel.getCurrentOrderList().getValue());
        });

        decrease.setOnClickListener(v -> {
            if (order.getQuantity() > 1) {
                order.setQuantity(order.getQuantity() - 1);
//                orderViewModel.updateOrder(order);
                orderViewModel.updateOrderInMemory(order);

                quantity.setText(String.valueOf(order.getQuantity()));
                updateTotal(orderViewModel.getCurrentOrderList().getValue());
            }
        });

        orderContainer.addView(itemView);
    }

    private void updateTotal(List<Order> orders) {
        double total = orderViewModel.calculateTotal(orders);
        totalTextView.setText(String.format("Total: $%.2f", total));
    }
}
