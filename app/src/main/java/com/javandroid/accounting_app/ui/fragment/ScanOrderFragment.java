package com.javandroid.accounting_app.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.Order;
import com.javandroid.accounting_app.data.model.Product;
import com.javandroid.accounting_app.databinding.FragmentScanOrderBinding;
import com.javandroid.accounting_app.ui.adapter.OrderAdapter;
import com.javandroid.accounting_app.ui.viewmodel.OrderViewModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanOrderFragment extends Fragment {

    private FragmentScanOrderBinding binding;
    private OrderViewModel viewModel;
    private OrderAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentScanOrderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(OrderViewModel.class);

        setupRecyclerView();
        setupListeners();
        observeViewModel();
    }


    private void setupRecyclerView() {
        adapter = new OrderAdapter(
                order -> viewModel.updateQuantity(order.getProductId(), order.getQuantity() + 1),
                order -> viewModel.updateQuantity(order.getProductId(), Math.max(order.getQuantity() - 1, 1))
        );

        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setupListeners() {
        binding.btnAddManual.setOnClickListener(v -> {
            String barcode = binding.editTextBarcode.getText().toString().trim();
            if (!barcode.isEmpty()) {
                fetchProductAndAdd(barcode);
                binding.editTextBarcode.setText("");
            }
        });

        binding.btnConfirmOrder.setOnClickListener(v -> {
            viewModel.confirmOrder();
            Toast.makeText(getContext(), "Order confirmed!", Toast.LENGTH_SHORT).show();
        });
    }

    private void observeViewModel() {
        viewModel.getCurrentOrders().observe(getViewLifecycleOwner(), orders -> {
            adapter.submitList(orders);
            binding.textTotal.setText("Total: $" + viewModel.calculateTotal());
        });
    }

    private void fetchProductAndAdd(String barcode) {
        executor.execute(() -> {
            Product product = viewModel.getProductByBarcode(barcode);  // This must not block the main thread

            mainHandler.post(() -> {
                if (product != null) {
                    Order order = new Order(
                            viewModel.getCurrentUserId(),
                            product.getId(),
                            product.getName(),
                            product.getBarcode(),
                            1,
                            product.getSellPrice(),
                            product.getBuyPrice()
                    );
                    viewModel.addOrUpdateProduct(order);
                } else {
//                    Toast.makeText(getContext(), "Product not found", Toast.LENGTH_SHORT).show();
                    openAddProductFragment(barcode);
                }
            });
        });
    }
    private void openAddProductFragment(String barcode) {
        Bundle args = new Bundle();
        args.putString("scanned_barcode", barcode);

        // Use Navigation Component
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.action_scanOrderFragment_to_addProductFragment, args);
    }


}
