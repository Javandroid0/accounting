package com.javandroid.accounting_app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.Product;
import com.javandroid.accounting_app.ui.adapter.ProductListAdapter;
import com.javandroid.accounting_app.ui.viewmodel.ProductViewModel;

import java.util.ArrayList;
import java.util.List;

public class AddProductFragment extends Fragment {

    private ProductViewModel productViewModel;
    private RecyclerView productRecyclerView;
    private ProductListAdapter productListAdapter;

    public AddProductFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_add_product, container, false);

        // Initialize the ProductViewModel
        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);

        // Set up the RecyclerView and Adapter
        productRecyclerView = rootView.findViewById(R.id.recyclerViewProductList);
        productRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapter with onItemClickListener to handle adding product to the order
        productListAdapter = new ProductListAdapter(new ArrayList<>(), product -> {
            // Handle adding product to order (you might need to navigate to OrderFragment here)
            // For example, use ViewModel to add the product to the order
            productViewModel.addProductToOrder(product);
        });
        productRecyclerView.setAdapter(productListAdapter);

        // Observe LiveData for product list updates
        productViewModel.getAllProducts().observe(getViewLifecycleOwner(), this::updateProductList);

        return rootView;
    }

    // Method to update the RecyclerView with the latest product list
    private void updateProductList(List<Product> products) {
        if (products != null) {
            productListAdapter.updateProductList(products);
        }
    }
}
