package com.javandroid.accounting_app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
    private EditText etProductName, etBarcode, etBuyPrice, etSellPrice, etDescription;
    private Button btnAddProduct;

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

        // Initialize the Adapter with onItemClickListener
        productListAdapter = new ProductListAdapter(new ArrayList<>(), product -> {
            // Handle adding product to order (you might need to navigate to OrderFragment here)
            // For example, use ViewModel to add the product to the order
            productViewModel.addProductToOrder(product);
        });
        productRecyclerView.setAdapter(productListAdapter);

        // Observe LiveData for product list updates
        productViewModel.getAllProducts().observe(getViewLifecycleOwner(), this::updateProductList);

        // Set up the EditText views and Button for adding a new product
        etProductName = rootView.findViewById(R.id.etProductName);
        etBarcode = rootView.findViewById(R.id.etBarcode);
        if (getArguments() != null) {
            String scannedBarcode = getArguments().getString("scanned_barcode", "");
            if (!scannedBarcode.isEmpty()) {
                etBarcode.setText(scannedBarcode);
                etBarcode.setEnabled(false); // optional: prevent editing
            }
        }
        etBuyPrice = rootView.findViewById(R.id.etBuyPrice);
        etSellPrice = rootView.findViewById(R.id.etSellPrice);
        etDescription = rootView.findViewById(R.id.etDescription);
        btnAddProduct = rootView.findViewById(R.id.btnAddProduct);

        // Handle adding a new product
        btnAddProduct.setOnClickListener(v -> {
            String productName = etProductName.getText().toString().trim();
            String barcode = etBarcode.getText().toString().trim();
            String buyPriceStr = etBuyPrice.getText().toString().trim();
            String sellPriceStr = etSellPrice.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (productName.isEmpty() || barcode.isEmpty() || buyPriceStr.isEmpty() || sellPriceStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all required fields.", Toast.LENGTH_SHORT).show();
            } else {
                double buyPrice = Double.parseDouble(buyPriceStr);
                double sellPrice = Double.parseDouble(sellPriceStr);

                // Create a new Product object
                Product newProduct = new Product();
                newProduct.setName(productName);
                newProduct.setBarcode(barcode);
                newProduct.setBuyPrice(buyPrice);
                newProduct.setSellPrice(sellPrice);
                newProduct.setDescription(description);

                // Add the new product to the database
                productViewModel.insertProduct(newProduct);

                // Optionally clear the input fields after adding
                etProductName.setText("");
                etBarcode.setText("");
                etBuyPrice.setText("");
                etSellPrice.setText("");
                etDescription.setText("");

                // Show a success message
                Toast.makeText(getContext(), "Product added successfully!", Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    // Method to update the RecyclerView with the latest product list
    private void updateProductList(List<Product> products) {
        if (products != null) {
            productListAdapter.updateProductList(products);
        }
    }
}
