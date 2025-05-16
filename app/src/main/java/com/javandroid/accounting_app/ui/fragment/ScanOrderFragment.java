package com.javandroid.accounting_app.ui.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.javandroid.accounting_app.MainActivity;
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.CustomerEntity;
import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.model.ProductEntity;
import com.javandroid.accounting_app.data.model.UserEntity;
import com.javandroid.accounting_app.databinding.FragmentScanOrderBinding;
import com.javandroid.accounting_app.ui.adapter.OrderEditorAdapter;
import com.javandroid.accounting_app.ui.viewmodel.CustomerViewModel;
import com.javandroid.accounting_app.ui.viewmodel.OrderViewModel;
import com.javandroid.accounting_app.ui.viewmodel.ProductViewModel;
import com.javandroid.accounting_app.ui.viewmodel.UserViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanOrderFragment extends Fragment implements OrderEditorAdapter.OnOrderItemChangeListener {

    private FragmentScanOrderBinding binding;
    private OrderViewModel orderViewModel;
    private ProductViewModel productViewModel;
    private CustomerViewModel customerViewModel;
    private UserViewModel userViewModel;
    private OrderEditorAdapter adapter;
    private TextInputEditText barcodeInput;
    private CustomerEntity selectedCustomer;
    private UserEntity currentUser;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final int PERMISSION_REQUEST_BLUETOOTH = 1001;
    private static final String[] BLUETOOTH_PERMISSIONS = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private final ActivityResultLauncher<Intent> barcodeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                IntentResult intentResult = IntentIntegrator.parseActivityResult(result.getResultCode(),
                        result.getData());
                if (intentResult != null && intentResult.getContents() != null) {
                    fetchProductAndAdd(intentResult.getContents().trim());
                } else {
                    Toast.makeText(getContext(), "Scan cancelled", Toast.LENGTH_SHORT).show();
                }
                refocusBarcodeInput();
            });

    private final ActivityResultLauncher<String[]> requestBluetoothPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted) {
                    // All permissions granted, proceed with printing
                    doPrintOrder(false);
                } else {
                    // Some permissions were denied
                    Toast.makeText(requireContext(),
                            "Bluetooth permissions are required for printing",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentScanOrderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViewModels();
        initViews(view);
        setupRecyclerView(view);
        setupListeners(view);
        observeViewModels();

        // Set focus to barcode input after creation
        refocusBarcodeInput();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Always set focus to barcode input when resuming
        refocusBarcodeInput();
    }

    private void initViewModels() {
        orderViewModel = new ViewModelProvider(requireActivity()).get(OrderViewModel.class);
        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);
        customerViewModel = new ViewModelProvider(requireActivity()).get(CustomerViewModel.class);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
    }

    private void initViews(View view) {
        barcodeInput = view.findViewById(R.id.editTextBarcode);
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OrderEditorAdapter(this);
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners(View view) {
        MaterialButton btnAddManual = view.findViewById(R.id.btnAddManual);
        MaterialButton btnConfirmOrder = view.findViewById(R.id.btnConfirmOrder);
        MaterialButton btnPrintOrder = view.findViewById(R.id.btnPrintOrder);

        btnAddManual.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(requireActivity());
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            integrator.setPrompt("Scan a barcode");
            integrator.setCameraId(0); // Use default camera
            integrator.setBeepEnabled(true);
            integrator.setBarcodeImageEnabled(true);
            barcodeLauncher.launch(integrator.createScanIntent());
        });

        btnConfirmOrder.setOnClickListener(v -> confirmOrder());
        btnPrintOrder.setOnClickListener(v -> printOrder());

        // Update the confirm button with the current total
        OrderEntity initialOrder = orderViewModel.getCurrentOrder().getValue();
        if (initialOrder != null) {
            updateConfirmButtonText(btnConfirmOrder, initialOrder.getTotal());
        }

        barcodeInput.setOnEditorActionListener((v, actionId, event) -> {
            handleBarcodeInput();
            return true;
        });

        // Add click listeners to the user and customer cards to open drawers
        view.findViewById(R.id.cardUser).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openUserDrawer();
            }
        });

        view.findViewById(R.id.cardCustomer).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCustomerDrawer();
            }
        });
    }

    private void observeViewModels() {
        // Observe current user
        userViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            currentUser = user;
            if (user != null) {
                orderViewModel.setCurrentUserId(user.getUserId());
                updateUserDisplay(user);
            }
        });

        // Observe selected customer
        customerViewModel.getSelectedCustomer().observe(getViewLifecycleOwner(), customer -> {
            selectedCustomer = customer;
            updateCustomerDisplay(customer);
        });

        // Observe current order items
        orderViewModel.getCurrentOrderItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                // Create a new list to force adapter update
                List<OrderItemEntity> itemsList = new ArrayList<>(items);
                adapter.submitList(null); // First clear the list
                adapter.submitList(itemsList); // Then set the new list
                updateTotalDisplay();
            }
        });

        // Observe scanned products
        orderViewModel.getLastScannedProduct().observe(getViewLifecycleOwner(), product -> {
            if (product != null) {
                orderViewModel.addProduct(product, 1);
            } else {
                String barcode = barcodeInput.getText().toString().trim();
                if (!barcode.isEmpty()) {
                    openAddProductFragment(barcode);
                } else {
                    Toast.makeText(requireContext(), "Please enter a barcode", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void handleBarcodeInput() {
        String barcode = barcodeInput.getText().toString().trim();
        if (!barcode.isEmpty()) {
            // Store the barcode in a final variable to use it in the lambda
            final String finalBarcode = barcode;

            // Clear input field immediately to prevent double-processing
            barcodeInput.setText("");

            // Use executor to run database query on background thread
            executor.execute(() -> {
                try {
                    // This runs on a background thread
                    ProductEntity product = productViewModel.getProductByBarcodeSync(finalBarcode);

                    // Update UI on main thread
                    mainHandler.post(() -> {
                        if (product != null) {
                            // Default quantity to add
                            double quantityToAdd = 1.0;

                            // Check if there's enough stock
                            if (product.getStock() >= quantityToAdd) {
                                // Product found, add to order
                                orderViewModel.addProduct(product, quantityToAdd);

                                Toast.makeText(requireContext(),
                                        "Product added: " + product.getName(),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(),
                                        "Product out of stock: " + product.getName(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Product not found, open add product screen
                            Toast.makeText(requireContext(),
                                    "Product not found. Please add details.",
                                    Toast.LENGTH_SHORT).show();
                            openAddProductFragment(finalBarcode);
                        }
                        // Input field was already cleared
                    });
                } catch (Exception e) {
                    // Handle any errors
                    mainHandler.post(() -> {
                        Toast.makeText(requireContext(),
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void updateCustomerDisplay(CustomerEntity customer) {
        if (getView() != null) {
            String customerText = customer != null ? customer.getName() : "No customer selected";
            TextView customerNameView = getView().findViewById(R.id.textViewCustomerName);
            if (customerNameView != null) {
                customerNameView.setText(customerText);
            }
        }
    }

    private void updateUserDisplay(UserEntity user) {
        if (getView() != null) {
            String userText = user != null ? user.getUsername() : "No user selected";
            TextView userNameView = getView().findViewById(R.id.textViewUserName);
            if (userNameView != null) {
                userNameView.setText(userText);
            }
        }
    }

    private void updateTotalDisplay() {
        if (getView() != null) {
            OrderEntity currentOrder = orderViewModel.getCurrentOrder().getValue();
            if (currentOrder != null) {
                // Update both the total display and the confirm button
                // String totalText = String.format("Total: $%.2f", currentOrder.getTotal());
                // TextView totalView = getView().findViewById(R.id.textTotal);
                // if (totalView != null) {
                // totalView.setText(totalText);
                // }

                // Also update the confirm button text
                MaterialButton btnConfirmOrder = getView().findViewById(R.id.btnConfirmOrder);
                updateConfirmButtonText(btnConfirmOrder, currentOrder.getTotal());
            }
        }
    }

    /**
     * Updates the confirm button text to include the total amount
     */
    private void updateConfirmButtonText(MaterialButton button, double total) {
        if (button != null) {
            // Format total as integer if it's a whole number
            button.setText(String.format("تایید %.3f", total));
        }
    }

    private void confirmOrder() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please select a user first", Toast.LENGTH_SHORT).show();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openUserDrawer();
            }
            return;
        }

        if (selectedCustomer == null) {
            Toast.makeText(requireContext(), "Please select a customer first", Toast.LENGTH_SHORT).show();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCustomerDrawer();
            }
            return;
        }

        OrderEntity currentOrder = orderViewModel.getCurrentOrder().getValue();
        if (currentOrder != null) {
            currentOrder.setCustomerId(selectedCustomer.getCustomerId());
            currentOrder.setUserId(currentUser.getUserId());

            // Confirm order but stay on this screen
            orderViewModel.confirmOrder();
            Toast.makeText(requireContext(), "Order confirmed successfully", Toast.LENGTH_SHORT).show();

            // Focus on barcode input to start a new order immediately
            refocusBarcodeInput();

            // Remove the navigation up that was causing us to leave the page
            // Navigation.findNavController(requireView()).navigateUp();
        }
    }

    private void printOrder() {
        // First, check if the order is confirmed (has ID)
        OrderEntity currentOrder = orderViewModel.getCurrentOrder().getValue();
        if (currentOrder != null && currentOrder.getOrderId() == 0) {
            // Order is not confirmed yet, confirm it first and then print
            if (currentUser == null) {
                Toast.makeText(requireContext(), "Please select a user first", Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openUserDrawer();
                }
                return;
            }

            if (selectedCustomer == null) {
                Toast.makeText(requireContext(), "Please select a customer first", Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openCustomerDrawer();
                }
                return;
            }

            // Set customer and user IDs
            currentOrder.setCustomerId(selectedCustomer.getCustomerId());
            currentOrder.setUserId(currentUser.getUserId());

            // Confirm order and then print
            orderViewModel.confirmOrderAndThen(() -> {
                // This callback runs after order is confirmed and will print the order
                checkBluetoothPermissionsAndPrint(false);
            });
        } else {
            // Order already has an ID, simply print it
            checkBluetoothPermissionsAndPrint(false);
        }
    }

    private void checkBluetoothPermissionsAndPrint(boolean isPreview) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12+ (API 31+)
            boolean allPermissionsGranted = true;
            for (String permission : BLUETOOTH_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(requireContext(),
                        permission) != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                doPrintOrder(isPreview);
            } else {
                requestBluetoothPermissionLauncher.launch(BLUETOOTH_PERMISSIONS);
            }
        } else {
            // For older Android versions
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
                    ||
                    ContextCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(requireActivity(),
                        new String[] {
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        },
                        PERMISSION_REQUEST_BLUETOOTH);
            } else {
                doPrintOrder(isPreview);
            }
        }
    }

    private void doPrintOrder(boolean isPreview) {
        // Check prerequisites on main thread
        if (currentUser == null) {
            mainHandler.post(() -> Toast.makeText(requireContext(),
                    "Please select a user first", Toast.LENGTH_SHORT).show());
            return;
        }

        if (selectedCustomer == null) {
            mainHandler.post(() -> Toast.makeText(requireContext(),
                    "Please select a customer", Toast.LENGTH_SHORT).show());
            return;
        }

        OrderEntity currentOrder = orderViewModel.getCurrentOrder().getValue();
        List<OrderItemEntity> items = orderViewModel.getCurrentOrderItems().getValue();

        if (currentOrder == null || items == null || items.isEmpty()) {
            mainHandler.post(() -> Toast.makeText(requireContext(),
                    "No items to print", Toast.LENGTH_SHORT).show());
            return;
        }

        executor.execute(() -> {
            try {
                BluetoothConnection printerConnection = BluetoothPrintersConnections.selectFirstPaired();
                if (printerConnection == null) {
                    mainHandler.post(() -> Toast.makeText(requireContext(),
                            "No printer found. Please connect a printer first.",
                            Toast.LENGTH_LONG).show());
                    return;
                }

                // Create receipt with optimal width and minimal feeds
                EscPosPrinter printer = new EscPosPrinter(printerConnection, 203, 48f, 32);

                // Format the date compactly
                String dateTime = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(new Date());

                // Build receipt with improved structure
                StringBuilder receiptText = new StringBuilder();

                // Header section
                receiptText.append("[C]<b>RECEIPT</b>\n");
                receiptText.append("[R]").append(dateTime).append("\n");
                receiptText.append("[L]Cust: ").append(selectedCustomer.getName()).append("\n");

                // Always show the order ID - will be 0 if it's a draft
                receiptText.append("[L]Order :").append(currentOrder.orderId).append("\n");

                receiptText.append("[C]--------------------\n");

                // Column headers
                receiptText.append("[L]ITEM[R]QTY PRICE TOTAL\n");
                receiptText.append("[C]----------------\n");

                // Items section - each on a single line
                for (OrderItemEntity item : items) {
                    double total = item.getSellPrice() * item.getQuantity();

                    // Truncate name if necessary for alignment
                    String productName = item.getProductName();
                    if (productName.length() > 12) {
                        productName = productName.substring(0, 10) + "..";
                    }

                    // Format item line with aligned values - check if quantity is whole number
                    String quantityFormat = (item.getQuantity() == Math.floor(item.getQuantity()))
                            ? "%.0f $%.2f $%.2f\n"
                            : "%.1f $%.2f $%.2f\n";

                    receiptText.append("[L]").append(productName)
                            .append("[R]").append(String.format(quantityFormat,
                                    item.getQuantity(),
                                    item.getSellPrice(),
                                    total));
                }

                // Total section
                receiptText.append("[C]--------------------\n");
                receiptText.append("[R]<b>TOTAL: $").append(String.format("%.2f</b>\n", currentOrder.getTotal()));
                receiptText.append("[C]Thanks!");

                // Print all text at once
                printer.printFormattedText(receiptText.toString());

                mainHandler.post(() -> Toast.makeText(requireContext(),
                        "Receipt printed successfully",
                        Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> Toast.makeText(requireContext(),
                        "Error printing receipt: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onQuantityChanged(OrderItemEntity item, double newQuantity) {
        // Get the current quantity to calculate difference
        double currentQuantity = item.getQuantity();
        double quantityDifference = newQuantity - currentQuantity;

        // If quantity is increasing, we need to decrease stock
        if (quantityDifference > 0 && item.getProductId() != null) {
            executor.execute(() -> {
                ProductEntity product = productViewModel.getProductByIdSync(item.getProductId());
                if (product != null) {
                    // Check if we have enough stock
                    if (product.getStock() >= quantityDifference) {
                        // Decrease stock by the difference
                        product.setStock(product.getStock() - quantityDifference);
                        productViewModel.updateProduct(product);

                        // Update quantity in the order item
                        mainHandler.post(() -> orderViewModel.updateQuantity(item, newQuantity));
                    } else {
                        // Not enough stock
                        mainHandler.post(() -> {
                            Toast.makeText(requireContext(),
                                    "Not enough stock available. Only " + product.getStock() + " left.",
                                    Toast.LENGTH_SHORT).show();

                            // Reset quantity field to current value
                            adapter.notifyDataSetChanged();
                        });
                    }
                } else {
                    // Product not found, but still update quantity
                    mainHandler.post(() -> orderViewModel.updateQuantity(item, newQuantity));
                }
            });
        } else {
            // If quantity is decreasing, we need to add back to stock
            if (quantityDifference < 0 && item.getProductId() != null) {
                executor.execute(() -> {
                    ProductEntity product = productViewModel.getProductByIdSync(item.getProductId());
                    if (product != null) {
                        // Increase stock by the absolute difference
                        product.setStock(product.getStock() + Math.abs(quantityDifference));
                        productViewModel.updateProduct(product);
                    }

                    // Update quantity in the order item
                    mainHandler.post(() -> orderViewModel.updateQuantity(item, newQuantity));
                });
            } else {
                // No change in quantity or no product ID
                orderViewModel.updateQuantity(item, newQuantity);
            }
        }
    }

    @Override
    public void onPriceChanged(OrderItemEntity item, double newPrice) {
        // Update price logic if needed
    }

    @Override
    public void onDelete(OrderItemEntity item) {
        // Use the remove method in OrderViewModel
        orderViewModel.removeItem(item);

        // If needed, return quantity to stock
        if (item.getProductId() != null) {
            executor.execute(() -> {
                ProductEntity product = productViewModel.getProductByIdSync(item.getProductId());
                if (product != null) {
                    // Add the quantity back to stock
                    product.setStock(product.getStock() + item.getQuantity());
                    productViewModel.updateProduct(product);
                }
            });
        }
    }

    private void fetchProductAndAdd(String barcode) {
        // This should be treated the same way as handleBarcodeInput
        if (barcode != null && !barcode.isEmpty()) {
            final String finalBarcode = barcode;

            // Use executor to run database query on background thread
            executor.execute(() -> {
                try {
                    // This runs on a background thread
                    ProductEntity product = productViewModel.getProductByBarcodeSync(finalBarcode);

                    // Update UI on main thread
                    mainHandler.post(() -> {
                        if (product != null) {
                            // Check if there's enough stock
                            if (product.getStock() > 0) {
                                // Product found, add to order
                                orderViewModel.addProduct(product, 1);

                                Toast.makeText(requireContext(),
                                        "Product added: " + product.getName(),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(),
                                        "Product out of stock: " + product.getName(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Product not found, open add product screen
                            Toast.makeText(requireContext(),
                                    "Product not found. Please add details.",
                                    Toast.LENGTH_SHORT).show();
                            openAddProductFragment(finalBarcode);
                        }
                    });
                } catch (Exception e) {
                    // Handle any errors
                    mainHandler.post(() -> {
                        Toast.makeText(requireContext(),
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void openAddProductFragment(String barcode) {
        try {
            // Get the current destination ID
            NavController navController = Navigation.findNavController(requireView());
            int currentDestinationId = navController.getCurrentDestination().getId();

            // Only navigate if we're on the ScanOrderFragment
            if (currentDestinationId == R.id.scanOrderFragment) {
                Bundle args = new Bundle();
                args.putString("barcode", barcode);
                navController.navigate(R.id.action_scanOrderFragment_to_addProductFragment, args);
            } else {
                // We're already on another screen, just show a toast
                Toast.makeText(requireContext(),
                        "Cannot navigate to add product from current screen",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            // Handle any navigation errors gracefully
            Toast.makeText(requireContext(),
                    "Navigation error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void refocusBarcodeInput() {
        binding.editTextBarcode.postDelayed(() -> {
            binding.editTextBarcode.requestFocus();
            binding.editTextBarcode.setSelection(binding.editTextBarcode.getText().length());
        }, 150);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_BLUETOOTH) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                doPrintOrder(false); // Default to non-preview mode
            } else {
                Toast.makeText(requireContext(),
                        "Bluetooth permissions are required for printing",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
