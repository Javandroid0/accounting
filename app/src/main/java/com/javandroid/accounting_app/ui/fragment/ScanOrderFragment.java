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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.Order;
import com.javandroid.accounting_app.data.model.Product;
import com.javandroid.accounting_app.databinding.FragmentScanOrderBinding;
import com.javandroid.accounting_app.ui.adapter.OrderAdapter;
import com.javandroid.accounting_app.ui.viewmodel.CustomerViewModel;
import com.javandroid.accounting_app.ui.viewmodel.OrderViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ScanOrderFragment extends Fragment {

    private FragmentScanOrderBinding binding;
    private OrderViewModel viewModel;
    private OrderAdapter adapter;

    private CustomerViewModel customerViewModel;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> barcodeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                IntentResult intentResult = IntentIntegrator.parseActivityResult(result.getResultCode(), result.getData());
                if (intentResult.getContents() != null) {
                    fetchProductAndAdd(intentResult.getContents().trim());
                } else {
                    Toast.makeText(getContext(), "Scan cancelled", Toast.LENGTH_SHORT).show();
                }
                refocusBarcodeInput();
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentScanOrderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(OrderViewModel.class);
        customerViewModel = new ViewModelProvider(requireActivity()).get(CustomerViewModel.class);

        setupRecyclerView();
        setupListeners();
        binding.btnPrintOrder.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ensureBluetoothPermissions();
            }

            List<Order> orders = viewModel.getCurrentOrders().getValue();
            if (orders == null || orders.isEmpty()) {
                Toast.makeText(getContext(), "No orders to print", Toast.LENGTH_SHORT).show();
                return;
            }

            String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

            StringBuilder receipt = new StringBuilder();
            receipt.append("[C]<b>Order Receipt</b>\n");
            receipt.append("[C]").append(currentDateTime).append("\n");
            receipt.append("[C]-----------------------------\n");
            receipt.append("[L]Qty | Item           | Total\n");

            for (Order order : orders) {
                double totalPrice = order.getQuantity() * order.getProductSellPrice();
                receipt.append("[L]")
                        .append(order.getQuantity()).append("             X ")
                        .append(order.getProductName())
                        .append(" [R]")
                        .append(totalPrice)
                        .append("\n");
            }

            receipt.append("[C]-----------------------------\n");
            receipt.append("[R]Total: ").append(viewModel.calculateTotal()).append("\n");
            receipt.append("\n[C]Thank you!\n\n");

            printReceipt(receipt.toString());
        });


        observeViewModel();
        observeCustomer();
        refocusBarcodeInput();


    }


    private void ensureBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            List<String> permissionsToRequest = new ArrayList<>();

            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
            }

            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
            }

            if (!permissionsToRequest.isEmpty()) {
                ActivityCompat.requestPermissions(requireActivity(),
                        permissionsToRequest.toArray(new String[0]), 1001);
            }
        }
    }


    private void printReceipt(String text) {
        try {
            BluetoothConnection printerConnection = BluetoothPrintersConnections.selectFirstPaired();
            if (printerConnection == null) {
                Toast.makeText(getContext(), "پرینتر بلوتوث پیدا نشد", Toast.LENGTH_SHORT).show();
                return;
            }

            EscPosPrinter printer = new EscPosPrinter(printerConnection, 203, 48f, 32);
            printer.printFormattedText(text);
            Toast.makeText(getContext(), "چاپ انجام شد", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "خطا در چاپ: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private void observeCustomer() {
        customerViewModel.getSelectedCustomer().observe(getViewLifecycleOwner(), customer -> {
            if (customer != null) {
                binding.textViewCustomerName.setText("Current: " + customer.getName());
            } else {
                binding.textViewCustomerName.setText("Current: None");
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new OrderAdapter(
                order -> viewModel.updateQuantity(order.getProductId(), order.getQuantity() + 1),
                order -> {
                    if (order.getQuantity() <= 1) {
                        viewModel.deleteOrder1(order.getProductId());
                    } else {
                        viewModel.updateQuantity(order.getProductId(), order.getQuantity() - 1);
                    }
                },
                (order, newQuantity) -> viewModel.updateQuantity(order.getProductId(), newQuantity),
                order -> viewModel.deleteOrder1(order.getProductId())
        );

        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setupListeners() {
        binding.editTextBarcode.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER) {
                String barcode = binding.editTextBarcode.getText().toString().trim();
                if (!barcode.isEmpty()) {
                    fetchProductAndAdd(barcode);
                    binding.editTextBarcode.setText("");
                    binding.editTextBarcode.requestFocus();
                }
                return true;
            }
            return false;
        });

        binding.btnAddManual.setOnClickListener(v -> {
            IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            integrator.setPrompt("Scan a barcode");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(true);
            integrator.setBarcodeImageEnabled(true);
            barcodeLauncher.launch(integrator.createScanIntent());
        });

        binding.btnConfirmOrder.setOnClickListener(v -> {
            viewModel.confirmOrder();
            Toast.makeText(getContext(), "Order confirmed!", Toast.LENGTH_SHORT).show();
            adapter.submitList(null); // Optional: clear UI list after confirmation
            refocusBarcodeInput();
//            List<Order> confirmedOrders = viewModel.getCurrentOrders().getValue();
//            if (confirmedOrders == null || confirmedOrders.isEmpty()) {
//                Toast.makeText(getContext(), "No items to confirm", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            viewModel.confirmOrder(); // still saves to DB
//
//// Create receipt text
//            StringBuilder receiptBuilder = new StringBuilder();
//            receiptBuilder.append("[C]<b>Order Receipt</b>\n");
//            receiptBuilder.append("[C]-----------------------------\n");
//
//            for (Order order : confirmedOrders) {
//                receiptBuilder.append("[L]")
//                        .append(order.getProductName())
//                        .append(" x")
//                        .append(order.getQuantity())
//                        .append(" - ")
//                        .append(order.getProductSellPrice())
//                        .append("\n");
//            }
//
//            receiptBuilder.append("[C]-----------------------------\n");
//            receiptBuilder.append("[R]Total: ").append(viewModel.calculateTotal()).append("\n");
//            receiptBuilder.append("\n\n[C]Thank you!\n\n\n");
//
//// Call print method
//            printViaBluetooth(receiptBuilder.toString());
//
//            Toast.makeText(getContext(), "Order confirmed!", Toast.LENGTH_SHORT).show();
//            adapter.submitList(null); // Clear UI list after confirmation
//            refocusBarcodeInput();

        });
    }


//    private void printViaBluetooth(String text) {
//        try {
//            BluetoothConnection printerConnection = BluetoothPrintersConnections.selectFirstPaired();
//            if (printerConnection == null) {
//                Toast.makeText(getContext(), "No Bluetooth printer found", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            EscPosPrinter printer = new EscPosPrinter(printerConnection, 203, 48f, 32);
//            printer.printFormattedText(text);
//        } catch (Exception e) {
//            e.printStackTrace();
//            Toast.makeText(getContext(), "Printing failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
//        }
//    }

    private void observeViewModel() {
        viewModel.getCurrentOrders().observe(getViewLifecycleOwner(), orders -> {
            adapter.submitList(orders); // ListAdapter handles diffing and animations
//            adapter.submitList(new ArrayList<>(orders));
            binding.textTotal.setText("جمع: " + viewModel.calculateTotal());
        });

    }

    private void fetchProductAndAdd(String barcode) {
        executor.execute(() -> {
            Product product = viewModel.getProductByBarcode(barcode);
            mainHandler.post(() -> {
                if (product != null) {
                    System.out.println(product.getName());
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
                    openAddProductFragment(barcode);
                }
                refocusBarcodeInput();
            });
        });
    }

    private void openAddProductFragment(String barcode) {
        Bundle args = new Bundle();
        args.putString("scanned_barcode", barcode);
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.action_scanOrderFragment_to_addProductFragment, args);
    }

    private void refocusBarcodeInput() {
        binding.editTextBarcode.postDelayed(() -> {
            binding.editTextBarcode.requestFocus();
            binding.editTextBarcode.setSelection(binding.editTextBarcode.getText().length());
        }, 150);
    }
}
