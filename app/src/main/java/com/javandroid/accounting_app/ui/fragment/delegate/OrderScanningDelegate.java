package com.javandroid.accounting_app.ui.fragment.delegate;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.databinding.FragmentScanOrderBinding;
import com.javandroid.accounting_app.ui.viewmodel.ProductScanViewModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Delegate class to handle barcode scanning and product operations
 * Helps to reduce complexity in ScanOrderFragment
 */
public class OrderScanningDelegate {
    private static final String TAG = "OrderScanningDelegate";

    private final Fragment fragment;
    private final FragmentScanOrderBinding binding;
    private final ProductScanViewModel productScanViewModel;
    private final TextInputEditText barcodeInput;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ActivityResultLauncher<Intent> barcodeLauncher;

    public OrderScanningDelegate(Fragment fragment, FragmentScanOrderBinding binding,
            ProductScanViewModel viewModel, TextInputEditText barcodeInput) {
        this.fragment = fragment;
        this.binding = binding;
        this.productScanViewModel = viewModel;
        this.barcodeInput = barcodeInput;

        setupBarcodeLauncher();
    }

    /**
     * Setup the barcode scanner launcher
     */
    private void setupBarcodeLauncher() {
        barcodeLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    IntentResult intentResult = IntentIntegrator.parseActivityResult(result.getResultCode(),
                            result.getData());
                    if (intentResult != null && intentResult.getContents() != null) {
                        fetchProductAndAdd(intentResult.getContents().trim());
                    } else {
                        Toast.makeText(fragment.getContext(), "Scan cancelled", Toast.LENGTH_SHORT).show();
                    }
                    refocusBarcodeInput();
                });
    }

    /**
     * Start the barcode scanner
     */
    public void startBarcodeScanner() {
        IntentIntegrator integrator = new IntentIntegrator(fragment.requireActivity());
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt("Scan a barcode");
        integrator.setCameraId(0); // Use default camera
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        barcodeLauncher.launch(integrator.createScanIntent());
    }

    /**
     * Handle manual barcode input
     */
    public void handleBarcodeInput() {
        String barcode = barcodeInput.getText().toString().trim();
        if (!barcode.isEmpty()) {
            // Clear input field immediately to prevent double-processing
            barcodeInput.setText("");

            // Use ProductScanViewModel to process the barcode
            fetchProductAndAdd(barcode);
        }
    }

    /**
     * Process a barcode to find and add the product
     */
    public void fetchProductAndAdd(String barcode) {
        // Use ProductScanViewModel
        if (barcode != null && !barcode.isEmpty()) {
            try {
                productScanViewModel.addProductByBarcode(barcode, 1.0);
            } catch (Exception e) {
                Log.e(TAG, "Error adding product: " + e.getMessage());
                Toast.makeText(fragment.requireContext(),
                        "Error adding product: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Setup observers for product scan events
     */
    public void setupScanObservers() {
        // Observe product not found event
        productScanViewModel.getProductNotFoundEvent().observe(fragment.getViewLifecycleOwner(), barcode -> {
            if (barcode != null && !barcode.isEmpty()) {
                // Show confirmation dialog
                showProductNotFoundDialog(barcode);
            }
        });

        // Observe product operation messages
        productScanViewModel.getProductOperationMessage().observe(fragment.getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(fragment.requireContext(), message.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Observe scanner active state
        productScanViewModel.getScannerActiveState().observe(fragment.getViewLifecycleOwner(), isActive -> {
            if (isActive) {
                // Re-enable barcode input or scanner as needed
                barcodeInput.setEnabled(true);
                refocusBarcodeInput();
            }
        });
    }

    /**
     * Show dialog asking if user wants to add a product that was not found
     */
    private void showProductNotFoundDialog(String barcode) {
        new AlertDialog.Builder(fragment.requireContext())
                .setTitle("Product Not Found")
                .setMessage("Product with barcode " + barcode + " not found. Do you want to add it?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Continue with add product flow
                    openAddProductFragment(barcode);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Cancel and return to scanning
                    productScanViewModel.cancelProductAddFlow();
                    dialog.dismiss();
                })
                .setCancelable(false) // Prevent dismissing without a choice
                .show();
    }

    /**
     * Navigate to the add product fragment with the given barcode
     */
    private void openAddProductFragment(String barcode) {
        try {
            // Set scanner inactive while in add product flow
            productScanViewModel.setScannerActive(false);

            // Create arguments bundle
            Bundle args = new Bundle();
            args.putString("barcode", barcode);

            // Use Navigation to navigate safely
            NavController navController = Navigation.findNavController(fragment.requireView());
            int currentDestinationId = navController.getCurrentDestination().getId();

            if (currentDestinationId == R.id.scanOrderFragment) {
                // Navigate with specified action
                navController.navigate(R.id.action_scanOrderFragment_to_addProductFragment, args);
            } else {
                // We're already on another screen, just show a toast
                Toast.makeText(fragment.requireContext(),
                        "Cannot navigate to add product from current screen",
                        Toast.LENGTH_SHORT).show();

                // Re-enable scanner since we couldn't navigate
                productScanViewModel.setScannerActive(true);
            }
        } catch (Exception e) {
            // Handle any navigation errors gracefully
            Toast.makeText(fragment.requireContext(),
                    "Navigation error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();

            // Re-enable scanner on error
            productScanViewModel.setScannerActive(true);
        }
    }

    /**
     * Refocus the barcode input field
     */
    public void refocusBarcodeInput() {
        binding.editTextBarcode.postDelayed(() -> {
            binding.editTextBarcode.requestFocus();
            binding.editTextBarcode.setSelection(binding.editTextBarcode.getText().length());
        }, 150);
    }

    /**
     * Clean up resources when the delegate is no longer needed
     */
    public void onDestroy() {
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }
}