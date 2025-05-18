package com.javandroid.accounting_app.ui.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.CustomerEntity;
import com.javandroid.accounting_app.ui.adapter.CustomerListAdapter;
import com.javandroid.accounting_app.ui.viewmodel.CustomerViewModel;

import java.util.List;

public class CustomerSelectionDialogFragment extends DialogFragment
        implements CustomerListAdapter.CustomerClickListener {

    private static final String TAG = "CustomerSelectionDialog";
    public static final String ARG_MODE = "mode";
    public static final String MODE_DELETE = "delete";
    public static final String MODE_EDIT = "edit";

    private CustomerViewModel customerViewModel;
    private CustomerListAdapter adapter;
    private String mode;

    public static CustomerSelectionDialogFragment newInstance(String mode) {
        CustomerSelectionDialogFragment fragment = new CustomerSelectionDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mode = getArguments().getString(ARG_MODE, MODE_EDIT);
        }
        customerViewModel = new ViewModelProvider(requireActivity()).get(CustomerViewModel.class);
        Log.d(TAG, "DialogFragment created with mode: " + mode);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_customer_selection, null);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewCustomers);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new CustomerListAdapter(this);
        recyclerView.setAdapter(adapter);

        // Prefetch customers so we have them when the dialog shows
        List<CustomerEntity> customers = customerViewModel.getAllCustomersSync();
        if (customers != null) {
            Log.d(TAG, "Pre-loaded " + customers.size() + " customers");
            adapter.submitList(customers);
        } else {
            Log.d(TAG, "No customers loaded synchronously");
        }

        String title = mode.equals(MODE_DELETE) ? "Select Customer to Delete" : "Select Customer to Edit";
        builder.setTitle(title)
                .setView(view)
                .setNegativeButton("Cancel", (dialog, id) -> dismiss());

        return builder.create();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Observe customers from ViewModel
        customerViewModel.getAllCustomers().observe(getViewLifecycleOwner(), customers -> {
            if (customers != null) {
                Log.d(TAG, "Observed " + customers.size() + " customers");
                adapter.submitList(customers);
            } else {
                Log.d(TAG, "Observed null customers list");
            }
        });
    }

    @Override
    public void onCustomerClick(CustomerEntity customer) {
        if (customer == null) {
            Log.e(TAG, "Clicked on null customer");
            return;
        }

        Log.d(TAG, "Customer clicked: " + customer.getName() + " (ID: " + customer.getCustomerId() + ")");

        if (mode.equals(MODE_DELETE)) {
            showDeleteConfirmation(customer);
        } else {
            // Set the customer for editing
            customerViewModel.setSelectedCustomer(customer);
            // Navigate to customer edit screen
            dismiss();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new CustomerEditFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void showDeleteConfirmation(CustomerEntity customer) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete " + customer.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    customerViewModel.delete(customer);
                    dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}