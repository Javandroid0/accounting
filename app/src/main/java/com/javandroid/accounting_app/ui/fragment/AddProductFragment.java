package com.javandroid.accounting_app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.textfield.TextInputEditText;
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.ProductEntity;
import com.javandroid.accounting_app.ui.viewmodel.ProductViewModel;

public class AddProductFragment extends Fragment {

    private ProductViewModel productViewModel;
    private TextInputEditText etBarcode;
    private TextInputEditText etProductName;
    private TextInputEditText etBuyPrice;
    private TextInputEditText etSellPrice;
    private TextInputEditText etStock;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_product, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupListeners(view);
        handleArguments();
    }

    private void initViews(View view) {
        etBarcode = view.findViewById(R.id.etBarcode);
        etProductName = view.findViewById(R.id.etProductName);
        etBuyPrice = view.findViewById(R.id.etBuyPrice);
        etSellPrice = view.findViewById(R.id.etSellPrice);
        etStock = view.findViewById(R.id.etStock);
    }

    private void setupListeners(View view) {
        view.findViewById(R.id.btnAddProduct).setOnClickListener(v -> saveProduct());
    }

    private void handleArguments() {
        Bundle args = getArguments();
        if (args != null && args.containsKey("barcode")) {
            String barcode = args.getString("barcode");
            etBarcode.setText(barcode);
        }
    }

    private void saveProduct() {
        String barcode = etBarcode.getText().toString().trim();
        String name = etProductName.getText().toString().trim();
        String buyPriceStr = etBuyPrice.getText().toString().trim();
        String sellPriceStr = etSellPrice.getText().toString().trim();
        String stockStr = etStock.getText().toString().trim();

        if (validateInput(barcode, name, buyPriceStr, sellPriceStr, stockStr)) {
            ProductEntity product = new ProductEntity(name, barcode);
            product.setBuyPrice(Double.parseDouble(buyPriceStr));
            product.setSellPrice(Double.parseDouble(sellPriceStr));
            product.setStock(Integer.parseInt(stockStr));

            productViewModel.insert(product);
            Toast.makeText(requireContext(), "Product added successfully", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).navigateUp();
        }
    }

    private boolean validateInput(String barcode, String name, String buyPrice, String sellPrice, String stock) {
        if (barcode.isEmpty()) {
            showError("Please enter a barcode");
            return false;
        }
        if (name.isEmpty()) {
            showError("Please enter a product name");
            return false;
        }
        if (buyPrice.isEmpty()) {
            showError("Please enter a buy price");
            return false;
        }
        if (sellPrice.isEmpty()) {
            showError("Please enter a sell price");
            return false;
        }
        if (stock.isEmpty()) {
            showError("Please enter stock quantity");
            return false;
        }

        try {
            double buy = Double.parseDouble(buyPrice);
            double sell = Double.parseDouble(sellPrice);
            int quantity = Integer.parseInt(stock);

            if (buy < 0) {
                showError("Buy price cannot be negative");
                return false;
            }
            if (sell < 0) {
                showError("Sell price cannot be negative");
                return false;
            }
            if (quantity < 0) {
                showError("Stock quantity cannot be negative");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Please enter valid numbers");
            return false;
        }

        return true;
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
