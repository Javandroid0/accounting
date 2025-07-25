package com.javandroid.accounting_app.ui.fragment.order;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
// androidx.core.app.ActivityCompat; // Not directly used, Fragment's requestPermissions is used
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.OrderEntity;
// OrderItemEntity and OrderEditorAdapter are not directly used by this fragment anymore
// import com.javandroid.accounting_app.data.model.OrderItemEntity;
// import com.javandroid.accounting_app.ui.adapter.order_editor.OrderEditorAdapter;
import com.javandroid.accounting_app.ui.adapter.order.SavedOrdersAdapter;
import com.javandroid.accounting_app.ui.viewmodel.order.SavedOrdersViewModel;
import com.javandroid.accounting_app.ui.viewmodel.order.OrderEditViewModel; // For observing potential delete events

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Removed: implements OrderEditorAdapter.OnOrderItemChangeListener

public class OrderEditorFragment extends Fragment {

    private static final String TAG = "OrderEditorFragment";
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 1001;

    private SavedOrdersViewModel savedOrdersViewModel;
    //    private OrderEditViewModel orderEditViewModel; // To observe events like order deletion
    private SavedOrdersAdapter savedOrdersAdapter;
    private RecyclerView recyclerViewOrders;
    private TextView emptyStateTextView;
    private EditText etSearchOrders;
    private Button btnExportCsv;
    //    private Button btnRefreshList;
    private ChipGroup chipGroupSort;

    private ChipGroup chipGroupSearchField;

    private SavedOrdersAdapter.SearchField currentSearchField = SavedOrdersAdapter.SearchField.ALL;

    private Handler mainThreadHandler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        savedOrdersViewModel = new ViewModelProvider(requireActivity()).get(SavedOrdersViewModel.class);
//        orderEditViewModel = new ViewModelProvider(requireActivity()).get(OrderEditViewModel.class); // For observing events
        mainThreadHandler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupListeners();
        observeViewModels();
    }

    private void initViews(View view) {
        recyclerViewOrders = view.findViewById(R.id.recycler_orders);
        emptyStateTextView = view.findViewById(R.id.empty_state_text);
        etSearchOrders = view.findViewById(R.id.et_search);
        btnExportCsv = view.findViewById(R.id.btn_export_csv);
        chipGroupSort = view.findViewById(R.id.chip_group_sort);
        chipGroupSearchField = view.findViewById(R.id.chip_group_search_field);
//        btnRefreshList = view.findViewById(R.id.btn_save_changes); // Layout ID is btn_save_changes
//        btnRefreshList.setText("Refresh List"); // Update button text
    }

    private void setupRecyclerView() {
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        savedOrdersAdapter = new SavedOrdersAdapter(order -> {
            // Navigate to OrderDetailsFragment for editing
            Log.d(TAG, "Navigating to details for order ID: " + order.getOrderId());
            Bundle args = new Bundle();
            args.putLong("orderId", order.getOrderId());
            if (getView() != null) {
                Navigation.findNavController(getView()).navigate(
                        R.id.action_orderEditorFragment_to_orderDetailsFragment, args);
            }
        });
        recyclerViewOrders.setAdapter(savedOrdersAdapter);
    }

    private void setupListeners() {
        btnExportCsv.setOnClickListener(v -> {
            if (checkStoragePermissions()) {
                exportOrdersToCSV();
            }
        });

//        btnRefreshList.setOnClickListener(v -> refreshOrderList());

        // Set listener for the chip group to handle sorting
        chipGroupSort.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_sort_date) {
                savedOrdersViewModel.changeSortOrder(SavedOrdersViewModel.SortType.BY_DATE);
            } else if (checkedId == R.id.chip_sort_total) {
                savedOrdersViewModel.changeSortOrder(SavedOrdersViewModel.SortType.BY_TOTAL);
            } else if (checkedId == R.id.chip_sort_customer) {
                savedOrdersViewModel.changeSortOrder(SavedOrdersViewModel.SortType.BY_CUSTOMER);
            }
        });

        etSearchOrders.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                Toast.makeText(requireContext(),
                        "Enter Order ID to search. Clear to see all.",
                        Toast.LENGTH_SHORT).show();
            }
        });
        chipGroupSearchField.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_search_id) {
                currentSearchField = SavedOrdersAdapter.SearchField.ID;
            } else if (checkedId == R.id.chip_search_date) {
                currentSearchField = SavedOrdersAdapter.SearchField.DATE;
            } else if (checkedId == R.id.chip_search_customer) {
                currentSearchField = SavedOrdersAdapter.SearchField.CUSTOMER;
            } else if (checkedId == R.id.chip_search_user) {
                currentSearchField = SavedOrdersAdapter.SearchField.USER;
            } else {
                currentSearchField = SavedOrdersAdapter.SearchField.ALL;
            }
            // Trigger a new filter operation with the current text
            String query = etSearchOrders.getText().toString();
            savedOrdersAdapter.filter(query, currentSearchField);
            updateEmptyState(savedOrdersAdapter.getItemCount() == 0, query);
        });

        // Set listener for the sort chip group
        chipGroupSort.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_sort_date) {
                savedOrdersViewModel.changeSortOrder(SavedOrdersViewModel.SortType.BY_DATE);
            } else if (checkedId == R.id.chip_sort_total) {
                savedOrdersViewModel.changeSortOrder(SavedOrdersViewModel.SortType.BY_TOTAL);
            } else if (checkedId == R.id.chip_sort_customer) {
                savedOrdersViewModel.changeSortOrder(SavedOrdersViewModel.SortType.BY_CUSTOMER);
            }
        });

        etSearchOrders.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString();
                // Pass the currentSearchField to the adapter
                savedOrdersAdapter.filter(query, currentSearchField);
                updateEmptyState(savedOrdersAdapter.getItemCount() == 0, query);
            }
        });
    }

    private void observeViewModels() {
        // Observe the new sortedOrders LiveData
        savedOrdersViewModel.getSortedOrders().observe(getViewLifecycleOwner(), orders -> {
            if (orders != null) {
                Log.d(TAG, "Observed " + orders.size() + " orders from ViewModel.");

                // The list is already sorted by the ViewModel.
                // We just need to update the adapter's master list and then apply the current filter.
                String currentQuery = etSearchOrders.getText().toString();
                savedOrdersAdapter.submitList(orders);
                savedOrdersAdapter.filter(currentQuery, currentSearchField);
                updateEmptyState(savedOrdersAdapter.getItemCount() == 0, currentQuery);
            }
        });
    }

    private void updateEmptyState(boolean isEmpty, String query) {
        if (isEmpty) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            if (query != null && !query.isEmpty()) {
                emptyStateTextView.setText("No orders found for: \"" + query + "\"");
            } else {
                emptyStateTextView.setText("No orders found.");
            }
        } else {
            emptyStateTextView.setVisibility(View.GONE);
        }
    }

    private boolean checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No explicit permission needed for app-specific directory or MediaStore on Q+
            // if writing to public directories like Downloads, different rules apply.
            // For getExternalFilesDir(null), no permission is needed.
            // For Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            // it's more complex and often requires SAF or MediaStore API.
            // Let's assume we use getExternalFilesDir for simplicity of permissions.
            return true;
        }
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportOrdersToCSV();
            } else {
                Toast.makeText(requireContext(),
                        "Storage permission is required to export data.",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void exportOrdersToCSV() {
        List<OrderEntity> ordersToExport = savedOrdersAdapter.getCurrentList(); // Get currently displayed/filtered list
        if (ordersToExport == null || ordersToExport.isEmpty()) {
            Toast.makeText(getContext(), "No orders to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "orders_export_" + timestamp + ".csv";
        File exportFile;

        // Use app-specific external files directory (no special permission needed on API 19+)
        File documentsDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (documentsDir == null) {
            Toast.makeText(getContext(), "Error accessing storage directory.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!documentsDir.exists()) {
            documentsDir.mkdirs();
        }
        exportFile = new File(documentsDir, fileName);

        try (FileWriter writer = new FileWriter(exportFile)) {
            // CSV Header
            writer.append("Order ID,Date,Customer ID,User ID,Total\n");

            // Write order data
            for (OrderEntity order : ordersToExport) {
                writer.append(String.valueOf(order.getOrderId())).append(",");
                writer.append("\"").append(escapeCsvField(order.getDate())).append("\"").append(",");
                writer.append(String.valueOf(order.getCustomerId())).append(",");
                writer.append(String.valueOf(order.getUserId())).append(",");
                writer.append(String.format(Locale.US, "%.2f", order.getTotal())).append("\n");
            }
            writer.flush();
            Toast.makeText(getContext(), "Orders exported to: " + exportFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            Log.d(TAG, "CSV Exported to: " + exportFile.getAbsolutePath());

        } catch (IOException e) {
            Log.e(TAG, "Error exporting orders to CSV", e);
            Toast.makeText(getContext(), "Error exporting orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String escapeCsvField(String field) {
        if (field == null) return "";
        // Replace double quotes with two double quotes
        return field.replace("\"", "\"\"");
    }


    // Removed OnOrderItemChangeListener methods as OrderEditorAdapter is not used here.
    // void onQuantityChanged(OrderItemEntity item, double newQuantity);
    // void onPriceChanged(OrderItemEntity item, double newPrice);
    // void onDelete(OrderItemEntity item);

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up handler if it was posting delayed messages, etc.
        if (mainThreadHandler != null) {
            mainThreadHandler.removeCallbacksAndMessages(null);
        }
        recyclerViewOrders = null; // Release reference
        savedOrdersAdapter = null; // Release reference
    }
}