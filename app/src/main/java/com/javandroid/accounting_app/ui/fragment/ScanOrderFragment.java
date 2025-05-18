package com.javandroid.accounting_app.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.javandroid.accounting_app.MainActivity;
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.CustomerEntity;
import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.model.UserEntity;
import com.javandroid.accounting_app.databinding.FragmentScanOrderBinding;
import com.javandroid.accounting_app.ui.adapter.OrderEditorAdapter;
import com.javandroid.accounting_app.ui.fragment.delegate.OrderManagementDelegate;
import com.javandroid.accounting_app.ui.fragment.delegate.OrderPrintingDelegate;
import com.javandroid.accounting_app.ui.fragment.delegate.OrderScanningDelegate;
import com.javandroid.accounting_app.ui.viewmodel.CurrentOrderViewModel;
import com.javandroid.accounting_app.ui.viewmodel.CustomerOrderStateViewModel;
import com.javandroid.accounting_app.ui.viewmodel.CustomerViewModel;
import com.javandroid.accounting_app.ui.viewmodel.ProductScanViewModel;
import com.javandroid.accounting_app.ui.viewmodel.ProductViewModel;
import com.javandroid.accounting_app.ui.viewmodel.UserViewModel;

import java.util.List;

public class ScanOrderFragment extends Fragment {

    private static final String TAG = "ScanOrderFragment";
    private FragmentScanOrderBinding binding;

    // ViewModels
    private CurrentOrderViewModel currentOrderViewModel;
    private ProductScanViewModel productScanViewModel;
    private ProductViewModel productViewModel;
    private CustomerViewModel customerViewModel;
    private UserViewModel userViewModel;
    private CustomerOrderStateViewModel customerOrderStateViewModel;

    // UI components
    private TextInputEditText barcodeInput;
    private CustomerEntity selectedCustomer;
    private UserEntity currentUser;

    // Delegate objects to handle specific functionality
    private OrderScanningDelegate scanningDelegate;
    private OrderPrintingDelegate printingDelegate;
    private OrderManagementDelegate managementDelegate;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "DEBUG: ScanOrderFragment created");
    }

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
        Log.d(TAG, "DEBUG: ScanOrderFragment view created");

        initViewModels();
        initViews(view);
        initDelegates();
        setupRecyclerView(view);
        setupListeners(view);
        setupObservers();

        // Set focus to barcode input after creation
        scanningDelegate.refocusBarcodeInput();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "DEBUG: ScanOrderFragment resumed");

        // Check current order state
        OrderEntity currentOrder = currentOrderViewModel != null ? currentOrderViewModel.getCurrentOrder().getValue()
                : null;
        List<OrderItemEntity> currentItems = currentOrderViewModel != null
                ? currentOrderViewModel.getCurrentOrderItems().getValue()
                : null;

        Log.d(TAG, "DEBUG: Current order state on resume - " +
                "order: " + (currentOrder != null ? currentOrder.getOrderId() : "null") +
                ", customerId: " + (currentOrder != null ? currentOrder.getCustomerId() : "null") +
                ", items: " + (currentItems != null ? currentItems.size() : 0));

        // Always set focus to barcode input when resuming
        scanningDelegate.refocusBarcodeInput();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up delegate resources
        if (scanningDelegate != null)
            scanningDelegate.onDestroy();
        if (printingDelegate != null)
            printingDelegate.onDestroy();
        if (managementDelegate != null)
            managementDelegate.onDestroy();

        Log.d(TAG, "DEBUG: ScanOrderFragment destroyed");
    }

    private void initViewModels() {
        currentOrderViewModel = new ViewModelProvider(requireActivity()).get(CurrentOrderViewModel.class);
        productScanViewModel = new ViewModelProvider(requireActivity()).get(ProductScanViewModel.class);
        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);
        customerViewModel = new ViewModelProvider(requireActivity()).get(CustomerViewModel.class);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        customerOrderStateViewModel = new ViewModelProvider(requireActivity()).get(CustomerOrderStateViewModel.class);
        Log.d(TAG, "DEBUG: ViewModels initialized");
    }

    private void initViews(View view) {
        barcodeInput = binding.editTextBarcode;

        // Initially disable barcode input until customer and user are selected
        updateBarcodeInputState();
        Log.d(TAG, "DEBUG: Views initialized");
    }

    private void initDelegates() {
        // Initialize delegate objects
        scanningDelegate = new OrderScanningDelegate(this, binding, productScanViewModel, barcodeInput);
        printingDelegate = new OrderPrintingDelegate(this, currentOrderViewModel);
        managementDelegate = new OrderManagementDelegate(this, currentOrderViewModel,
                customerOrderStateViewModel, productViewModel);

        Log.d(TAG, "DEBUG: Delegates initialized");
    }

    private void setupRecyclerView(View view) {
        OrderEditorAdapter adapter = new OrderEditorAdapter(managementDelegate);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        // Set the adapter on the management delegate
        managementDelegate.setAdapter(adapter);

        Log.d(TAG, "DEBUG: RecyclerView setup completed");
    }

    private void setupListeners(View view) {
        MaterialButton btnAddManual = binding.btnAddManual;
        MaterialButton btnConfirmOrder = binding.btnConfirmOrder;
        MaterialButton btnPrintOrder = binding.btnPrintOrder;

        // Initially disable the Add Manual button
        btnAddManual.setEnabled(false);

        // Initially disable confirm and print buttons until there are items
        btnConfirmOrder.setEnabled(false);
        btnPrintOrder.setEnabled(false);

        // Use scanning delegate for barcode scanning
        btnAddManual.setOnClickListener(v -> scanningDelegate.startBarcodeScanner());

        // Use management delegate for order confirmation
        btnConfirmOrder.setOnClickListener(v -> managementDelegate.confirmOrder());

        // Use printing delegate for printing, but first prepare the order using
        // management delegate
        btnPrintOrder.setOnClickListener(v -> managementDelegate
                .prepareOrderForPrinting(() -> printingDelegate.checkPermissionsAndPrint(false)));

        // Handle barcode input using scanning delegate
        barcodeInput.setOnEditorActionListener((v, actionId, event) -> {
            scanningDelegate.handleBarcodeInput();
            return true;
        });

        // Add click listeners to the user and customer cards to open drawers
        binding.cardUser.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openUserDrawer();
            }
        });

        binding.cardCustomer.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCustomerDrawer();
            }
        });
    }

    private void setupObservers() {
        // Set up observers in delegates
        scanningDelegate.setupScanObservers();
        managementDelegate.setupOrderObservers();

        // Observe customer and user selection
        customerViewModel.getSelectedCustomer().observe(getViewLifecycleOwner(), customer -> {
            selectedCustomer = customer;
            // Update the customer in delegates
            printingDelegate.setCustomer(customer);
            managementDelegate.setCustomer(customer);
            updateCustomerDisplay(customer);
            updateBarcodeInputState();
        });

        userViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            currentUser = user;
            // Update the user in delegates
            printingDelegate.setUser(user);
            managementDelegate.setUser(user);
            updateUserDisplay(user);
            updateBarcodeInputState();
        });

        // Observe order items to enable/disable confirm and print buttons
        currentOrderViewModel.getCurrentOrderItems().observe(getViewLifecycleOwner(), items -> {
            boolean hasItems = items != null && !items.isEmpty();
            binding.btnConfirmOrder.setEnabled(hasItems);
            binding.btnPrintOrder.setEnabled(hasItems);
        });
    }

    private void updateCustomerDisplay(CustomerEntity customer) {
        String customerText = customer != null ? customer.getName() : "No customer selected";
        binding.textViewCustomerName.setText(customerText);
    }

    private void updateUserDisplay(UserEntity user) {
        String userText = user != null ? user.getUsername() : "No user selected";
        binding.textViewUserName.setText(userText);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        // Delegate permission handling to the printing delegate
        printingDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Update the barcode input field state based on customer and user selection
     */
    private void updateBarcodeInputState() {
        boolean enabled = (selectedCustomer != null && currentUser != null);
        barcodeInput.setEnabled(enabled);

        // Also update the Add Manual button state
        MaterialButton btnAddManual = binding.btnAddManual;
        if (btnAddManual != null) {
            btnAddManual.setEnabled(enabled);
        }

        if (!enabled) {
            barcodeInput.setHint("Select customer and user first");
        } else {
            barcodeInput.setHint("Scan barcode");
        }
    }
}
