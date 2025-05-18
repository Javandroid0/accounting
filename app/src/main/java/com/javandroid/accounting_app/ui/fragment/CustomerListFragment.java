package com.javandroid.accounting_app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.ui.adapter.CustomerListAdapter;
import com.javandroid.accounting_app.ui.viewmodel.CustomerViewModel;

public class CustomerListFragment extends Fragment {

    private CustomerViewModel customerViewModel;
    private CustomerListAdapter adapter;
    private RecyclerView recyclerView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        customerViewModel = new ViewModelProvider(requireActivity()).get(CustomerViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_customer_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewCustomers);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new CustomerListAdapter(customer -> {
            customerViewModel.setSelectedCustomer(customer);
            Navigation.findNavController(view)
                    .navigate(R.id.action_customerListFragment_to_customerDetailFragment);
        });

        recyclerView.setAdapter(adapter);

        customerViewModel.getAllCustomers().observe(getViewLifecycleOwner(), customers -> {
            if (customers != null) {
                adapter.submitList(customers);
            }
        });
    }
}