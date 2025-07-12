package com.javandroid.accounting_app.ui.fragment.customer;

import android.os.Bundle;
import android.util.Log;
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
import com.javandroid.accounting_app.data.model.UserEntity;
import com.javandroid.accounting_app.data.repository.UserRepository;
import com.javandroid.accounting_app.ui.adapter.newOrder.OrderListAdapter;
import com.javandroid.accounting_app.ui.viewmodel.customer.CustomerViewModel;
import com.javandroid.accounting_app.ui.viewmodel.order.SavedOrdersViewModel;
import com.javandroid.accounting_app.ui.viewmodel.order.OrderEditViewModel;
import com.javandroid.accounting_app.ui.viewmodel.customer.CustomerOrderStateViewModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerDetailFragment extends Fragment implements OrderListAdapter.OrderClickListener {
    private static final String TAG = "CustomerDetailFragment";

    private CustomerViewModel customerViewModel;
    private SavedOrdersViewModel savedOrdersViewModel;
    private OrderEditViewModel orderEditViewModel;
    private CustomerOrderStateViewModel customerOrderStateViewModel;
    private OrderListAdapter adapter;
    private TextView tvCustomerName;
    private TextView tvCurrentUser;
    private TextView tvCurrentUserProfit;
    private TextView tvUserTotalProfit;
    private RecyclerView recyclerViewOrders;
    private ExecutorService executor;
    private UserRepository userRepository;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        customerViewModel = new ViewModelProvider(requireActivity()).get(CustomerViewModel.class);
        savedOrdersViewModel = new ViewModelProvider(requireActivity()).get(SavedOrdersViewModel.class);
        orderEditViewModel = new ViewModelProvider(requireActivity()).get(OrderEditViewModel.class);
        customerOrderStateViewModel = new ViewModelProvider(requireActivity()).get(CustomerOrderStateViewModel.class);
        executor = Executors.newSingleThreadExecutor();
        userRepository = new UserRepository(requireContext());
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
        tvCurrentUser = view.findViewById(R.id.tvCurrentUser);
        tvCurrentUserProfit = view.findViewById(R.id.tvCurrentUserProfit);
        tvUserTotalProfit = view.findViewById(R.id.tvUserTotalProfit);
        recyclerViewOrders = view.findViewById(R.id.recyclerViewOrders);
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new OrderListAdapter(this);
        recyclerViewOrders.setAdapter(adapter);

        // Observe the selected customer and load their orders
        customerViewModel.getSelectedCustomer().observe(getViewLifecycleOwner(), customer -> {
            if (customer != null) {
                tvCustomerName.setText(customer.getName());
                loadCustomerOrders(customer);
                loadProfitData(customer);
            }
        });

        // Observe profit calculations
        savedOrdersViewModel.getUserProfit().observe(getViewLifecycleOwner(), profit -> {
            if (profit != null) {
                tvUserTotalProfit.setText(String.format("Total profit: %.2f", profit));
            }
        });

        savedOrdersViewModel.getUserCustomerProfit().observe(getViewLifecycleOwner(), profit -> {
            if (profit != null) {
                tvCurrentUserProfit.setText(String.format("With this customer: %.2f", profit));
            }
        });
    }

    private void loadProfitData(CustomerEntity customer) {
        // Get the current user ID from CustomerOrderStateViewModel
        executor.execute(() -> {
            try {
                // In a real app, you'd get the current user ID from a session or preference
                // For demo purposes, we'll use user ID 1
                long userId = 1; // You can replace this with actual current user ID

                // Get the user info
                UserEntity user = userRepository.getUserByIdSync(userId);
                if (user != null) {
                    requireActivity().runOnUiThread(() -> {
                        tvCurrentUser.setText("User: " + user.getUsername());
                    });

                    // Calculate profit for this user with this customer
                    savedOrdersViewModel.calculateProfitByUserAndCustomer(userId, customer.getCustomerId());

                    // Calculate total profit for this user
                    savedOrdersViewModel.calculateProfitByUser(userId);

                    Log.d(TAG, "Loaded profit data for user " + userId + " and customer " + customer.getCustomerId());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading profit data", e);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}