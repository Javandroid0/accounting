package com.javandroid.accounting_app.ui.fragment.delegate;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.javandroid.accounting_app.data.model.CustomerEntity;
import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.model.UserEntity;
import com.javandroid.accounting_app.ui.viewmodel.CurrentOrderViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Delegate class to handle order printing operations
 * Helps to reduce complexity in ScanOrderFragment
 */
public class OrderPrintingDelegate {
    private static final String TAG = "OrderPrintingDelegate";

    private final Fragment fragment;
    private final CurrentOrderViewModel currentOrderViewModel;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ActivityResultLauncher<String[]> requestBluetoothPermissionLauncher;
    private CustomerEntity selectedCustomer;
    private UserEntity currentUser;

    private static final String[] BLUETOOTH_PERMISSIONS = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    public OrderPrintingDelegate(Fragment fragment, CurrentOrderViewModel currentOrderViewModel) {
        this.fragment = fragment;
        this.currentOrderViewModel = currentOrderViewModel;
        setupPermissionLauncher();
    }

    /**
     * Set up the permission launcher for Bluetooth permissions
     */
    private void setupPermissionLauncher() {
        requestBluetoothPermissionLauncher = fragment.registerForActivityResult(
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
                        Toast.makeText(fragment.requireContext(),
                                "Bluetooth permissions are required for printing",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Set the customer for printing
     */
    public void setCustomer(CustomerEntity customer) {
        this.selectedCustomer = customer;
    }

    /**
     * Set the user for printing
     */
    public void setUser(UserEntity user) {
        this.currentUser = user;
    }

    /**
     * Check permissions and print the order
     * 
     * @param isPreview true if this is just a preview
     */
    public void checkPermissionsAndPrint(boolean isPreview) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12+ (API 31+)
            boolean allPermissionsGranted = true;
            for (String permission : BLUETOOTH_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(fragment.requireContext(),
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
            if (ContextCompat.checkSelfPermission(fragment.requireContext(),
                    Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(fragment.requireContext(),
                            Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
                    ||
                    ContextCompat.checkSelfPermission(fragment.requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(fragment.requireActivity(),
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

    // Permission request code for Bluetooth
    public static final int PERMISSION_REQUEST_BLUETOOTH = 1001;

    /**
     * Handle permission request results
     */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
                Toast.makeText(fragment.requireContext(),
                        "Bluetooth permissions are required for printing",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Execute the print job
     * 
     * @param isPreview true if this is just a preview
     */
    private void doPrintOrder(boolean isPreview) {
        // Check prerequisites on main thread
        if (currentUser == null) {
            mainHandler.post(() -> Toast.makeText(fragment.requireContext(),
                    "Please select a user first", Toast.LENGTH_SHORT).show());
            return;
        }

        if (selectedCustomer == null) {
            mainHandler.post(() -> Toast.makeText(fragment.requireContext(),
                    "Please select a customer", Toast.LENGTH_SHORT).show());
            return;
        }

        OrderEntity currentOrder = currentOrderViewModel.getCurrentOrder().getValue();
        List<OrderItemEntity> items = currentOrderViewModel.getCurrentOrderItems().getValue();

        if (currentOrder == null || items == null || items.isEmpty()) {
            mainHandler.post(() -> Toast.makeText(fragment.requireContext(),
                    "No items to print", Toast.LENGTH_SHORT).show());
            return;
        }

        executor.execute(() -> {
            try {
                BluetoothConnection printerConnection = BluetoothPrintersConnections.selectFirstPaired();
                if (printerConnection == null) {
                    mainHandler.post(() -> Toast.makeText(fragment.requireContext(),
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
                receiptText.append("[L]Order :").append(currentOrder.getOrderId()).append("\n");

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

                mainHandler.post(() -> Toast.makeText(fragment.requireContext(),
                        "Receipt printed successfully",
                        Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> Toast.makeText(fragment.requireContext(),
                        "Error printing receipt: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        });
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