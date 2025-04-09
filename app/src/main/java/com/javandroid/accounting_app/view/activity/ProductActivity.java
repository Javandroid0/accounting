package com.javandroid.accounting_app.view.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.model.Product;
import com.javandroid.accounting_app.service.excel.ProductExcelManager;
import com.javandroid.accounting_app.view.adapter.ProductAdapter;
import java.io.IOException;
import java.util.List;

public class ProductActivity extends AppCompatActivity {

    private ProductExcelManager productExcelManager;
    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private EditText editTextProductId, editTextBarcode, editTextName, editTextSellPrice, editTextBuyPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);

        // Initialize Excel Manager
        productExcelManager = new ProductExcelManager(this);

        // Initialize UI elements
        recyclerView = findViewById(R.id.recyclerViewProducts);
        editTextProductId = findViewById(R.id.editTextProductId);
        editTextBarcode = findViewById(R.id.editTextBarcode);
        editTextName = findViewById(R.id.editTextName);
        editTextSellPrice = findViewById(R.id.editTextSellPrice);
        editTextBuyPrice = findViewById(R.id.editTextBuyPrice);
        Button buttonAdd = findViewById(R.id.buttonAddProduct);
        Button buttonUpdate = findViewById(R.id.buttonUpdateProduct);
        Button buttonDelete = findViewById(R.id.buttonDeleteProduct);

        // Set RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        loadProducts();

        // Button Click Listeners
        buttonAdd.setOnClickListener(view -> addProduct());
        buttonUpdate.setOnClickListener(view -> updateProduct());
        buttonDelete.setOnClickListener(view -> deleteProduct());
    }

    private void loadProducts() {
        try {
            List<Product> productList = productExcelManager.readProducts();
            if (adapter == null) {
                adapter = new ProductAdapter(productList);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateProducts(productList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void addProduct() {
        try {
            int id = Integer.parseInt(editTextProductId.getText().toString().trim());
            String barcode = editTextBarcode.getText().toString().trim();
            String name = editTextName.getText().toString().trim();
            double sellPrice = Double.parseDouble(editTextSellPrice.getText().toString().trim());
            double buyPrice = Double.parseDouble(editTextBuyPrice.getText().toString().trim());

            Product newProduct = new Product(id, barcode, name, sellPrice, buyPrice);
            productExcelManager.addProduct(newProduct);
            Toast.makeText(this, "Product Added", Toast.LENGTH_SHORT).show();
            clearFields();
            loadProducts();
        } catch (Exception e) {
            Toast.makeText(this, "Invalid Input", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProduct() {
        try {
            int id = Integer.parseInt(editTextProductId.getText().toString().trim());
            String barcode = editTextBarcode.getText().toString().trim();
            String name = editTextName.getText().toString().trim();
            double sellPrice = Double.parseDouble(editTextSellPrice.getText().toString().trim());
            double buyPrice = Double.parseDouble(editTextBuyPrice.getText().toString().trim());

            Product updatedProduct = new Product(id, barcode, name, sellPrice, buyPrice);
            if (productExcelManager.updateProduct(updatedProduct)) {
                Toast.makeText(this, "Product Updated", Toast.LENGTH_SHORT).show();
                clearFields();
                loadProducts();
            } else {
                Toast.makeText(this, "Product Not Found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Invalid Input", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteProduct() {
        try {
            int id = Integer.parseInt(editTextProductId.getText().toString().trim());

            if (productExcelManager.deleteProduct(id)) {
                Toast.makeText(this, "Product Deleted", Toast.LENGTH_SHORT).show();
                clearFields();
                loadProducts();
            } else {
                Toast.makeText(this, "Product Not Found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Invalid Input", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearFields() {
        editTextProductId.setText("");
        editTextBarcode.setText("");
        editTextName.setText("");
        editTextSellPrice.setText("");
        editTextBuyPrice.setText("");
    }
}
