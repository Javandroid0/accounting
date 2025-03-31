package com.javandroid.accounting_app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.poi.ss.usermodel.*;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SearchResultsAdapter.OnProductUpdateListener {

    private EditText searchInput;
    private SearchResultsAdapter adapter;
    private List<SearchResult> searchResults;
    private Workbook workbook;
    private Sheet sheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button searchButton = findViewById(R.id.searchButton);
        searchInput = findViewById(R.id.searchInput);
        RecyclerView resultsRecyclerView = findViewById(R.id.resultsRecyclerView);

        searchResults = new ArrayList<>();
        adapter = new SearchResultsAdapter(searchResults, this);
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        resultsRecyclerView.setAdapter(adapter);

        // Load Excel file from assets
        loadExcelFile();

        searchButton.setOnClickListener(v -> {
            String query = searchInput.getText().toString();
            searchProduct(query);
        });
    }

    private void loadExcelFile() {
        try {
            InputStream inputStream = getAssets().open("products.xlsx");
            workbook = WorkbookFactory.create(inputStream);
            sheet = workbook.getSheetAt(0);
            Toast.makeText(this, "Database Loaded!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load database", Toast.LENGTH_SHORT).show();
        }
    }

    private void searchProduct(String query) {
        if (workbook == null) {
            Toast.makeText(this, "Database not loaded!", Toast.LENGTH_SHORT).show();
            return;
        }

        searchResults.clear();

        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.toString().toLowerCase().contains(query.toLowerCase())) {
                    searchResults.add(new SearchResult(cell.toString(), row.getRowNum(), cell.getColumnIndex()));
                }
            }
        }

        if (searchResults.isEmpty()) {
            Toast.makeText(this, "No product found", Toast.LENGTH_SHORT).show();
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onUpdate(SearchResult product, String newValue) {
        if (workbook == null) {
            Toast.makeText(this, "Database not loaded!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Update the Excel cell
            Row row = sheet.getRow(product.getRow());
            Cell cell = row.getCell(product.getColumn());
            cell.setCellValue(newValue);

            // Save changes
            FileOutputStream outputStream = openFileOutput("products.xlsx", MODE_PRIVATE);
            workbook.write(outputStream);
            outputStream.close();

            Toast.makeText(this, "Product updated!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to update product", Toast.LENGTH_SHORT).show();
        }
    }
}
