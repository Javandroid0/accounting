package com.javandroid.accounting_app.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
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

//        binding.editTextBarcode.requestFocus();


    }

    private final ActivityResultLauncher<Intent> barcodeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                IntentResult intentResult = IntentIntegrator.parseActivityResult(result.getResultCode(), result.getData());
                if (intentResult != null) {
                    if (intentResult.getContents() != null) {
                        String scannedBarcode = intentResult.getContents().trim();
                        fetchProductAndAdd(scannedBarcode);
                    } else {
                        Toast.makeText(getContext(), "Cancelled", Toast.LENGTH_SHORT).show();
                    }
//                    binding.editTextBarcode.requestFocus();
                }
            }
    );


//    @Override
//    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
//        if (result != null) {
//            System.out.println("aaa");
//            if (result.getContents() != null) {
//                String scannedBarcode = result.getContents().trim();
//                fetchProductAndAdd(scannedBarcode);
//            } else {
//                Toast.makeText(getContext(), "Cancelled", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }

    private void setupRecyclerView() {
        adapter = new OrderAdapter(
                order -> viewModel.updateQuantity(order.getProductId(), order.getQuantity() + 1),
                order -> viewModel.updateQuantity(order.getProductId(), Math.max(order.getQuantity() - 1, 1)),
                (order, newQuantity) -> viewModel.updateQuantity(order.getProductId(), newQuantity), // 🔥 Handle manual EditText update
//                order -> viewModel.deleteOrder(order.getProductId()) // ✅ Deletion logic
                order -> viewModel.deleteOrder1(order.getProductId())
        );


        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setupListeners() {
        // Keyboard listener for hardware scanner
        binding.editTextBarcode.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                String barcode = binding.editTextBarcode.getText().toString().trim();
                if (!barcode.isEmpty()) {
                    fetchProductAndAdd(barcode);
                    binding.editTextBarcode.setText("");  // 🔥 Clear for next scan
//                    binding.editTextBarcode.requestFocus();  // 🔥 Refocus for next scan
                }
                return true;
            }
            return false;
        });

        binding.btnAddManual.setOnClickListener(v -> {
            IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);  // ✅ Important change
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            integrator.setPrompt("Scan a barcode");
            integrator.setCameraId(0);  // Use a specific camera
            integrator.setBeepEnabled(true);
            integrator.setBarcodeImageEnabled(true);

            Intent intent = integrator.createScanIntent();  // Create the Intent manually
            barcodeLauncher.launch(intent);  // 🚀 Launch using ActivityResultLauncher
        });


        binding.btnConfirmOrder.setOnClickListener(v -> {
            viewModel.confirmOrder();
            Toast.makeText(getContext(), "Order confirmed!", Toast.LENGTH_SHORT).show();
        });
    }

    private void observeViewModel() {
        viewModel.getCurrentOrders().observe(getViewLifecycleOwner(), orders -> {

            adapter.submitList(orders);

            binding.textTotal.setText("جمع: " + viewModel.calculateTotal());
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
//                binding.editTextBarcode.requestFocus();  // 🔥 Refocus no matter what
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
