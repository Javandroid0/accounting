package com.javandroid.accounting_app.ui.fragment.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.ui.adapter.user.UserProfitAdapter;
import com.javandroid.accounting_app.ui.viewmodel.user.UserProfitViewModel;

public class UserProfitFragment extends Fragment {

    private UserProfitViewModel userProfitViewModel;
    private UserProfitAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvTotalProfit;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userProfitViewModel = new ViewModelProvider(requireActivity()).get(UserProfitViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_profit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerViewUserProfit);
        tvTotalProfit = view.findViewById(R.id.tvTotalProfit);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new UserProfitAdapter();
        recyclerView.setAdapter(adapter);

        // Set up observers
        userProfitViewModel.getUserProfitList().observe(getViewLifecycleOwner(), userProfitDataList -> {
            adapter.submitList(userProfitDataList);
        });

        userProfitViewModel.getTotalProfitAcrossAllUsers().observe(getViewLifecycleOwner(), totalProfit -> {
            tvTotalProfit.setText(getString(R.string.total_profit_format, totalProfit));
        });

        // Load profit data
        userProfitViewModel.loadAllUserProfits();
    }
}