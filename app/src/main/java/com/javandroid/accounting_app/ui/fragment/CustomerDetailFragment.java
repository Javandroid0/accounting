package com.javandroid.accounting_app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.CustomerEntity;
import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.ui.adapter.OrderListAdapter;
import com.javandroid.accounting_app.ui.viewmodel.CustomerViewModel;
import com.javandroid.accounting_app.ui.viewmodel.SavedOrdersViewModel;
import com.javandroid.accounting_app.ui.viewmodel.OrderEditViewModel;

public class CustomerDetailFragment extends Fragment implements OrderListAdapter.OrderClickListener {

    private CustomerViewModel customerViewModel;
    private SavedOrdersViewModel savedOrdersViewModel;
    private OrderEditViewModel orderEditViewModel;
    private OrderListAdapter adapter;
    private TextView tvCustomerName;
    private RecyclerView recyclerViewOrders;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        customerViewModel = new ViewModelProvider(requireActivity()).get(CustomerViewModel.class);
        savedOrdersViewModel = new ViewModelProvider(requireActivity()).get(SavedOrdersViewModel.class);
        orderEditViewModel = new ViewModelProvider(requireActivity()).get(OrderEditViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_customer_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvCustomerName = view.findViewById(R.id.tvCustomerName);
        recyclerViewOrders = view.findViewById(R.id.recyclerViewOrders);
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new OrderListAdapter(this);
        recyclerViewOrders.setAdapter(adapter);

        // Observe the selected customer and load their orders
        customerViewModel.getSelectedCustomer().observe(getViewLifecycleOwner(), customer -> {
            if (customer != null) {
                tvCustomerName.setText(customer.getName());
                loadCustomerOrders(customer);
            }
        });
    }

    private void loadCustomerOrders(CustomerEntity customer) {
        savedOrdersViewModel.getOrdersByCustomerId(customer.getCustomerId())
                .observe(getViewLifecycleOwner(), orders -> {
                    if (orders != null) {
                        adapter.submitList(orders);
                    }
                });
    }

    @Override
    public void onOrderClick(OrderEntity order) {
        // Set the order for editing using OrderEditViewModel
        orderEditViewModel.setEditingOrder(order);

        // Navigate to the order editor fragment
        Navigation.findNavController(requireView()).navigate(
                R.id.action_customerDetailFragment_to_orderEditorFragment);
    }
}