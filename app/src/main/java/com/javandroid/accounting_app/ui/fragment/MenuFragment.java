package com.javandroid.accounting_app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.javandroid.accounting_app.R;
import com.google.android.material.button.MaterialButton;

public class MenuFragment extends Fragment {

    public MenuFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton btnNewOrder = view.findViewById(R.id.btn_new_order);
        MaterialButton btnViewOrders = view.findViewById(R.id.btn_view_orders);
        MaterialButton btnProducts = view.findViewById(R.id.btn_products);
        MaterialButton btnCustomers = view.findViewById(R.id.btn_customers);

        btnNewOrder.setOnClickListener(
                v -> Navigation.findNavController(v).navigate(R.id.action_menuFragment_to_scanOrderFragment));

        btnViewOrders.setOnClickListener(
                v -> Navigation.findNavController(v).navigate(R.id.action_menuFragment_to_orderEditorFragment));

        btnProducts.setOnClickListener(
                v -> Navigation.findNavController(v).navigate(R.id.action_menuFragment_to_productEditorFragment));

        btnCustomers.setOnClickListener(
                v -> Navigation.findNavController(v).navigate(R.id.action_menuFragment_to_customerListFragment));
    }
}
