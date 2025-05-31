package com.javandroid.accounting_app.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText; // For general EditText focusing
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout; // For setting errors on TextInputLayout
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.data.model.ProductEntity;
import com.javandroid.accounting_app.ui.viewmodel.ProductViewModel;
import com.javandroid.accounting_app.ui.viewmodel.ProductScanViewModel;

public class AddProductFragment extends Fragment {

    private ProductViewModel productViewModel;
    private ProductScanViewModel productScanViewModel;

    private TextInputLayout tilBarcode, tilProductName, tilBuyPrice, tilSellPrice, tilStock;
    private TextInputEditText etBarcode;
    private TextInputEditText etProductName;
    private TextInputEditText etBuyPrice;
    private TextInputEditText etSellPrice;
    private TextInputEditText etStock;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        productViewModel = new ViewModelProvider(requireActivity()).get(ProductViewModel.class);
        productScanViewModel = new ViewModelProvider(requireActivity()).get(ProductScanViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_product, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupListeners(view);
        handleArguments(); // This will now also control etBarcode's enabled state
    }

    private void initViews(View view) {
        tilBarcode = view.findViewById(R.id.tilBarcode);
        etBarcode = view.findViewById(R.id.etBarcode);
        tilProductName = view.findViewById(R.id.tilProductName);
        etProductName = view.findViewById(R.id.etProductName);
        tilBuyPrice = view.findViewById(R.id.tilBuyPrice);
        etBuyPrice = view.findViewById(R.id.etBuyPrice);
        tilSellPrice = view.findViewById(R.id.tilSellPrice);
        etSellPrice = view.findViewById(R.id.etSellPrice);
        tilStock = view.findViewById(R.id.tilStock);
        etStock = view.findViewById(R.id.etStock);

        // RecyclerView with ID recyclerViewProductList is assumed to be removed from XML
    }

    private void setupListeners(View view) {
        view.findViewById(R.id.btnAddProduct).setOnClickListener(v -> saveProduct());
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> cancelAddProduct());
    }

    private void handleArguments() {
        Bundle args = getArguments();
        if (args != null && args.containsKey("barcode")) {
            String barcode = args.getString("barcode");
            if (barcode != null && !barcode.isEmpty()) {
                etBarcode.setText(barcode);
                etBarcode.setEnabled(false); // Disable if barcode is pre-filled
                tilBarcode.setEnabled(false); // Also disable TextInputLayout if desired
                etProductName.requestFocus(); // Move focus to the next logical field
            } else {
                etBarcode.setEnabled(true); // Enable if barcode from args is empty/null
                tilBarcode.setEnabled(true);
            }
        } else {
            etBarcode.setEnabled(true); // Enable if no barcode argument is passed (manual add)
            tilBarcode.setEnabled(true);
        }
    }

    private void saveProduct() {
        // Clear previous errors
        clearAllErrors();

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

            productViewModel.insertProduct(product); // Use insert from ProductViewModel
            Toast.makeText(requireContext(), "Product '" + name + "' added successfully", Toast.LENGTH_SHORT).show();

            // Re-enable scanner when returning to ScanOrderFragment
            productScanViewModel.setScannerActive(true);
            if (getView() != null) {
                Navigation.findNavController(getView()).navigateUp();
            }
        }
    }

    private void cancelAddProduct() {
        // Tell ProductScanViewModel to re-enable scanner if it was the source
        productScanViewModel.setScannerActive(true);
        // productScanViewModel.cancelProductAddFlow(); // This might also clear events if needed

        if (getView() != null) {
            Navigation.findNavController(getView()).navigateUp();
        }
    }

    private boolean validateInput(String barcode, String name, String buyPrice, String sellPrice, String stock) {
        if (TextUtils.isEmpty(barcode) && etBarcode.isEnabled()) { // Only validate if enabled
            showErrorOnTextInputLayout(tilBarcode, "Please enter a barcode", etBarcode);
            return false;
        }
        if (TextUtils.isEmpty(name)) {
            showErrorOnTextInputLayout(tilProductName, "Please enter a product name", etProductName);
            return false;
        }
        if (TextUtils.isEmpty(buyPrice)) {
            showErrorOnTextInputLayout(tilBuyPrice, "Please enter a buy price", etBuyPrice);
            return false;
        }
        if (TextUtils.isEmpty(sellPrice)) {
            showErrorOnTextInputLayout(tilSellPrice, "Please enter a sell price", etSellPrice);
            return false;
        }
        if (TextUtils.isEmpty(stock)) {
            showErrorOnTextInputLayout(tilStock, "Please enter stock quantity", etStock);
            return false;
        }

        try {
            double buy = Double.parseDouble(buyPrice);
            double sell = Double.parseDouble(sellPrice);
            int quantity = Integer.parseInt(stock);

            if (buy < 0) {
                showErrorOnTextInputLayout(tilBuyPrice, "Buy price cannot be negative", etBuyPrice);
                return false;
            }
            if (sell < 0) {
                showErrorOnTextInputLayout(tilSellPrice, "Sell price cannot be negative", etSellPrice);
                return false;
            }
            if (quantity < 0) {
                showErrorOnTextInputLayout(tilStock, "Stock quantity cannot be negative", etStock);
                return false;
            }
        } catch (NumberFormatException e) {
            // Attempt to identify which field caused the error if possible,
            // for now, a general message.
            Toast.makeText(requireContext(), "Please enter valid numbers for prices and stock.", Toast.LENGTH_LONG).show();
            // Check which field is likely problematic
            if (!isNumeric(buyPrice))
                showErrorOnTextInputLayout(tilBuyPrice, "Invalid number", etBuyPrice);
            else if (!isNumeric(sellPrice))
                showErrorOnTextInputLayout(tilSellPrice, "Invalid number", etSellPrice);
            else if (!isInteger(stock))
                showErrorOnTextInputLayout(tilStock, "Invalid integer", etStock);
            return false;
        }

        return true;
    }

    private boolean isNumeric(String str) {
        if (str == null) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isInteger(String str) {
        if (str == null) return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    private void showErrorOnTextInputLayout(TextInputLayout til, String message, EditText fieldToFocus) {
        til.setError(message);
        if (fieldToFocus != null) {
            fieldToFocus.requestFocus();
        }
    }

    private void clearAllErrors() {
        if (tilBarcode != null) tilBarcode.setError(null);
        if (tilProductName != null) tilProductName.setError(null);
        if (tilBuyPrice != null) tilBuyPrice.setError(null);
        if (tilSellPrice != null) tilSellPrice.setError(null);
        if (tilStock != null) tilStock.setError(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Nullify views to help prevent memory leaks, especially if not using ViewBinding
        tilBarcode = null;
        etBarcode = null;
        tilProductName = null;
        etProductName = null;
        tilBuyPrice = null;
        etBuyPrice = null;
        tilSellPrice = null;
        etSellPrice = null;
        tilStock = null;
        etStock = null;
    }
}