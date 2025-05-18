package com.javandroid.accounting_app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.CustomerEntity;
import com.javandroid.accounting_app.ui.viewmodel.CustomerViewModel;

public class CustomerEditFragment extends Fragment {
    private CustomerViewModel customerViewModel;
    private EditText etCustomerName;
    private Button btnSaveCustomer;
    private CustomerEntity currentCustomer;
    private boolean isEditing = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        customerViewModel = new ViewModelProvider(requireActivity()).get(CustomerViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_customer_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etCustomerName = view.findViewById(R.id.etCustomerName);
        btnSaveCustomer = view.findViewById(R.id.btnSaveCustomer);

        customerViewModel.getSelectedCustomer().observe(getViewLifecycleOwner(), customer -> {
            if (customer != null) {
                currentCustomer = customer;
                populateFields(customer);
                isEditing = true;
            }
        });

        btnSaveCustomer.setOnClickListener(v -> saveCustomer());
    }

    private void populateFields(CustomerEntity customer) {
        etCustomerName.setText(customer.getName());
    }

    private void saveCustomer() {
        String name = etCustomerName.getText().toString().trim();

        if (name.isEmpty()) {
            etCustomerName.setError("Name is required");
            return;
        }

        if (isEditing) {
            // Update existing customer
            CustomerEntity updatedCustomer = new CustomerEntity(name);
            updatedCustomer.setCustomerId(currentCustomer.getCustomerId());
            customerViewModel.update(updatedCustomer);
            Toast.makeText(requireContext(), "Customer updated successfully", Toast.LENGTH_SHORT).show();
        } else {
            // Create new customer
            CustomerEntity newCustomer = new CustomerEntity(name);
            customerViewModel.insert(newCustomer);
            Toast.makeText(requireContext(), "Customer created successfully", Toast.LENGTH_SHORT).show();
        }

        // Navigate back
        requireActivity().onBackPressed();
    }
}