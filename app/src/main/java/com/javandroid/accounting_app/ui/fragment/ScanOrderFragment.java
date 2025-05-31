package com.javandroid.accounting_app.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast; // Added for user feedback

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.javandroid.accounting_app.MainActivity;
// R class should be imported from your app's package
import com.javandroid.accounting_app.R;

import com.javandroid.accounting_app.data.model.CustomerEntity;
import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.model.UserEntity;
import com.javandroid.accounting_app.databinding.FragmentScanOrderBinding;
// Use ScanOrderAdapter
import com.javandroid.accounting_app.ui.adapter.ScanOrderAdapter;
import com.javandroid.accounting_app.ui.fragment.delegate.OrderManagementDelegate;
import com.javandroid.accounting_app.ui.fragment.delegate.OrderPrintingDelegate;
import com.javandroid.accounting_app.ui.fragment.delegate.OrderScanningDelegate;
import com.javandroid.accounting_app.ui.viewmodel.CurrentOrderViewModel;
import com.javandroid.accounting_app.ui.viewmodel.CustomerOrderStateViewModel;
import com.javandroid.accounting_app.ui.viewmodel.CustomerViewModel;
import com.javandroid.accounting_app.ui.viewmodel.ProductScanViewModel;
import com.javandroid.accounting_app.ui.viewmodel.ProductViewModel;
import com.javandroid.accounting_app.ui.viewmodel.UserViewModel;

import java.util.ArrayList; // For submitting new list to adapter
import java.util.List;

public class ScanOrderFragment extends Fragment {

    private static final String TAG = "ScanOrderFragment";
    private FragmentScanOrderBinding binding;

    private CurrentOrderViewModel currentOrderViewModel;
    private ProductScanViewModel productScanViewModel;
    private ProductViewModel productViewModel; // Keep if delegate uses it
    private CustomerViewModel customerViewModel;
    private UserViewModel userViewModel;
    private CustomerOrderStateViewModel customerOrderStateViewModel;

    private ScanOrderAdapter scanOrderAdapter; // Changed type
    private CustomerEntity selectedCustomer;
    private UserEntity currentUser;
    // private String currentUserId; // currentUser.getUserId() can be used

    private OrderScanningDelegate scanningDelegate;
    private OrderPrintingDelegate printingDelegate;
    private OrderManagementDelegate managementDelegate;
    // private boolean isInitialLoad = true; // Not currently used

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ScanOrderFragment created");
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
        Log.d(TAG, "ScanOrderFragment view created");

        initViewModels();
        // initViews now part of setupListeners or done via binding
        initDelegates(); // ManagementDelegate needs ViewModels
        setupRecyclerView(); // Pass the delegate as listener
        setupListeners();
        setupObservers();

        if (scanningDelegate != null) scanningDelegate.refocusBarcodeInput();
    }

    private void initViewModels() {
        currentOrderViewModel = new ViewModelProvider(requireActivity()).get(CurrentOrderViewModel.class);
        productScanViewModel = new ViewModelProvider(requireActivity()).get(ProductScanViewModel.class);
        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);
        customerViewModel = new ViewModelProvider(requireActivity()).get(CustomerViewModel.class);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        customerOrderStateViewModel = new ViewModelProvider(requireActivity()).get(CustomerOrderStateViewModel.class);
        Log.d(TAG, "ViewModels initialized");
    }

    private void initDelegates() {
        scanningDelegate = new OrderScanningDelegate(this, binding, productScanViewModel, binding.editTextBarcode);
        printingDelegate = new OrderPrintingDelegate(this, currentOrderViewModel);
        // ManagementDelegate is the listener for the adapter
        managementDelegate = new OrderManagementDelegate(this, currentOrderViewModel,
                customerOrderStateViewModel, productViewModel);
        Log.d(TAG, "Delegates initialized");
    }

    private void setupRecyclerView() {
        // managementDelegate now implements OrderItemInteractionListener
        scanOrderAdapter = new ScanOrderAdapter(managementDelegate);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(scanOrderAdapter);
        Log.d(TAG, "RecyclerView setup with ScanOrderAdapter completed");
    }

    private void setupListeners() {
        // Barcode input via binding.editTextBarcode
        updateBarcodeInputState(); // Initial state

        binding.btnAddManual.setOnClickListener(v -> {
            if (selectedCustomer != null && currentUser != null) {
                if (scanningDelegate != null) scanningDelegate.startBarcodeScanner();
            } else {
                Toast.makeText(getContext(), "Please select customer and user first.", Toast.LENGTH_SHORT).show();
            }
        });
        binding.btnConfirmOrder.setOnClickListener(v -> {
            if (managementDelegate != null) managementDelegate.confirmOrder();
        });
        binding.btnPrintOrder.setOnClickListener(v -> {
            if (managementDelegate != null && printingDelegate != null) {
                managementDelegate.prepareOrderForPrinting(() -> printingDelegate.checkPermissionsAndPrint(false));
            }
        });

        binding.editTextBarcode.setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) ||
                    actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO ||
                    actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_NULL) {
                if (scanningDelegate != null) {
                    scanningDelegate.handleBarcodeInput(); // This should clear the input
                    scanningDelegate.refocusBarcodeInput();
                }
                return true;
            }
            return false;
        });

        binding.cardUser.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).openUserDrawer();
        });
        binding.cardCustomer.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).openCustomerDrawer();
        });
    }

    private void setupObservers() {
        if (scanningDelegate != null) scanningDelegate.setupScanObservers();
        // ManagementDelegate setupOrderObservers might update total display, not adapter directly
        if (managementDelegate != null) managementDelegate.setupOrderObservers();

        customerViewModel.getSelectedCustomer().observe(getViewLifecycleOwner(), customer -> {
            selectedCustomer = customer;
            if (printingDelegate != null) printingDelegate.setCustomer(customer);
            if (managementDelegate != null) managementDelegate.setCustomer(customer);
            updateCustomerDisplay(customer);
            updateBarcodeInputState();
            if (customer != null && customerOrderStateViewModel != null) {
                customerOrderStateViewModel.setCustomerId(customer.getCustomerId(), false); // false = load state or clear for new customer
            }
        });

        userViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            currentUser = user;
            if (printingDelegate != null) printingDelegate.setUser(user);
            if (managementDelegate != null) managementDelegate.setUser(user);
            updateUserDisplay(user);
            updateBarcodeInputState();
            if (user != null && customerOrderStateViewModel != null) {
                customerOrderStateViewModel.setCurrentUserId(user.getUserId());
            }
        });

        currentOrderViewModel.getFragmentOrderItemsLiveData().observe(getViewLifecycleOwner(), items -> {
            boolean hasItems = items != null && !items.isEmpty();
            binding.btnConfirmOrder.setEnabled(hasItems);
            binding.btnPrintOrder.setEnabled(hasItems);
            if (scanOrderAdapter != null) {
                scanOrderAdapter.submitList(items != null ? new ArrayList<>(items) : new ArrayList<>());
            }
            if (managementDelegate != null) { // Update total when items change
                managementDelegate.updateTotalDisplay();

            }
        });
        // Observe current order to update total display (already handled by managementDelegate.setupOrderObservers)
        // currentOrderViewModel.getCurrentOrder().observe(getViewLifecycleOwner(), order -> {
        //     if (managementDelegate != null) managementDelegate.updateTotalDisplay();
        // });
    }

    private void updateCustomerDisplay(CustomerEntity customer) {
        binding.textViewCustomerName.setText(customer != null ? customer.getName() : "No customer selected");
    }

    private void updateUserDisplay(UserEntity user) {
        binding.textViewUserName.setText(user != null ? user.getUsername() : "No user selected");
    }

    private void updateBarcodeInputState() {
        boolean enabled = (selectedCustomer != null && currentUser != null);
        binding.editTextBarcode.setEnabled(enabled);
        binding.btnAddManual.setEnabled(enabled); // Camera button
        binding.editTextBarcode.setHint(enabled ? "Scan or type barcode" : "Select customer & user");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "ScanOrderFragment resumed");
        if (scanningDelegate != null) scanningDelegate.refocusBarcodeInput();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Delegates should handle their own cleanup if needed in their onDestroy methods
        binding = null; // Important for fragments with ViewBinding
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scanningDelegate != null) scanningDelegate.onDestroy();
        if (printingDelegate != null) printingDelegate.onDestroy();
        if (managementDelegate != null) managementDelegate.onDestroy();
        Log.d(TAG, "ScanOrderFragment destroyed");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // Important for child fragments
        if (printingDelegate != null) {
            printingDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        // Handle other permissions if any
    }
}