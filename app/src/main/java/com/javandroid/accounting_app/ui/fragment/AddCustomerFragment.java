package com.javandroid.accounting_app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.CustomerEntity;
import com.javandroid.accounting_app.ui.viewmodel.CustomerViewModel;

public class AddCustomerFragment extends Fragment {

    private CustomerViewModel customerViewModel;
    private EditText etName;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        customerViewModel = new ViewModelProvider(requireActivity()).get(CustomerViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_customer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etName = view.findViewById(R.id.etCustomerName);

        MaterialButton btnSave = view.findViewById(R.id.btnSaveCustomer);
        btnSave.setOnClickListener(v -> saveCustomer());

        MaterialButton btnCancel = view.findViewById(R.id.btnCancelCustomer);
        btnCancel.setOnClickListener(v -> Navigation.findNavController(requireView()).navigateUp());
    }

    private void saveCustomer() {
        String name = etName.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
            return;
        }

        CustomerEntity customer = new CustomerEntity();
        customer.setName(name);
        // We don't need these fields as per user's request
        // customer.setPhone(phone);
        // customer.setEmail(email);
        // customer.setAddress(address);

        customerViewModel.insert(customer);
        Toast.makeText(requireContext(), "Customer added successfully", Toast.LENGTH_SHORT).show();

        Navigation.findNavController(requireView()).navigateUp();
    }
}