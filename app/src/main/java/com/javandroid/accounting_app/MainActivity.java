package com.javandroid.accounting_app;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.javandroid.accounting_app.model.Order;
import com.javandroid.accounting_app.model.Product;
import com.javandroid.accounting_app.service.ExcelManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText searchInput, productNameInput, productBarcodeInput, productSellPriceInput, productBuyPriceInput;
    private Button searchButton, addProductButton, calculateProfitButton;
    private ListView listView;
    private ExcelManager excelManager;
    private ArrayAdapter<String> adapter;
    private List<Product> productList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Views
        searchInput = findViewById(R.id.searchInput);
        searchButton = findViewById(R.id.searchButton);
        productNameInput = findViewById(R.id.productNameInput);
        productBarcodeInput = findViewById(R.id.productBarcodeInput);
        productSellPriceInput = findViewById(R.id.productSellPriceInput);
        productBuyPriceInput = findViewById(R.id.productBuyPriceInput);
        addProductButton = findViewById(R.id.addProductButton);
        calculateProfitButton = findViewById(R.id.calculateProfitButton);
        listView = findViewById(R.id.listView);

        // Initialize Excel Manager
        excelManager = new ExcelManager(this);

        // Load product list initially
        loadProductList();

        // Search for a product
        searchButton.setOnClickListener(v -> searchProduct());

        // Add or update a product
        addProductButton.setOnClickListener(v -> addOrUpdateProduct());

        // Calculate total profit from orders
        calculateProfitButton.setOnClickListener(v -> calculateProfit());
    }

    private void loadProductList() {
        productList = excelManager.getAllProducts();
        updateListView();
    }

    private void updateListView() {
        String[] items = new String[productList.size()];
        for (int i = 0; i < productList.size(); i++) {
            Product p = productList.get(i);
            items[i] = p.getId() + ": " + p.getName() + " - " + p.getSellPrice();
        }
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        listView.setAdapter(adapter);
    }
//    private void updateListView() {
//        adapter = new ProductAdapter(this, productList);
//        listView.setAdapter(adapter);
//    }


    private void searchProduct() {
        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) {
            loadProductList();
            return;
        }

        productList = excelManager.getAllProducts();
        productList.removeIf(p -> !p.getName().toLowerCase().contains(query.toLowerCase()));
        updateListView();
    }

    private void addOrUpdateProduct() {
        String name = productNameInput.getText().toString().trim();
        String barcode = productBarcodeInput.getText().toString().trim();
        double sellPrice = Double.parseDouble(productSellPriceInput.getText().toString().trim());
        double buyPrice = Double.parseDouble(productBuyPriceInput.getText().toString().trim());

        if (name.isEmpty() || barcode.isEmpty()) {
            Toast.makeText(this, "Please enter valid product details", Toast.LENGTH_SHORT).show();
            return;
        }

        int id = productList.size() + 1; // Generate new ID
        Product product = new Product(id, name, barcode, sellPrice, buyPrice);
        excelManager.addOrUpdateProduct(product);
        Toast.makeText(this, "Product added/updated successfully", Toast.LENGTH_SHORT).show();
        loadProductList();
    }

    private void calculateProfit() {
        List<Order> orders = excelManager.getAllOrders();
        double totalProfit = 0;

        for (Order order : orders) {
            double profitPerItem = order.getSellPrice() - order.getBuyPrice();
            totalProfit += profitPerItem * order.getQuantity();
        }

        Toast.makeText(this, "Total Profit: $" + totalProfit, Toast.LENGTH_LONG).show();
    }
}
