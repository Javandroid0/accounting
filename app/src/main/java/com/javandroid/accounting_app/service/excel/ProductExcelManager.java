package com.javandroid.accounting_app.service.excel;

import android.content.Context;
import android.content.res.AssetManager;
import com.javandroid.accounting_app.model.Product;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ProductExcelManager {

    private static final String ASSET_FILE_NAME = "products.xlsx"; // Excel file in assets
    private static final String OUTPUT_FILE_NAME = "products.xlsx"; // Internal storage path

    private final Context context;

    public ProductExcelManager(Context context) {
        this.context = context;
    }

    // ✅ Read products from assets/products.xlsx
    public List<Product> readProducts() throws IOException {
        List<Product> products = new ArrayList<>();
        AssetManager assetManager = context.getAssets();
        InputStream fis = assetManager.open(ASSET_FILE_NAME);
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheetAt(0);

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // Skip header row

            int id = (int) row.getCell(0).getNumericCellValue();
            String barcode = row.getCell(1).getStringCellValue();
            String name = row.getCell(2).getStringCellValue();
            double sellPrice = row.getCell(3).getNumericCellValue();
            double buyPrice = row.getCell(4).getNumericCellValue();

            products.add(new Product(id, barcode, name, sellPrice, buyPrice));
        }

        workbook.close();
        fis.close();
        return products;
    }

    // ✅ Write products to internal storage
    public void writeProducts(List<Product> products) throws IOException {
        File outputFile = new File(context.getFilesDir(), OUTPUT_FILE_NAME); // Internal storage path
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Products");

        // Write header row
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("Barcode");
        header.createCell(2).setCellValue("Name");
        header.createCell(3).setCellValue("Sell Price");
        header.createCell(4).setCellValue("Buy Price");

        // Write data rows
        int rowNum = 1;
        for (Product product : products) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(product.getId());
            row.createCell(1).setCellValue(product.getBarcode());
            row.createCell(2).setCellValue(product.getName());
            row.createCell(3).setCellValue(product.getSellPrice());
            row.createCell(4).setCellValue(product.getBuyPrice());
        }

        FileOutputStream fos = new FileOutputStream(outputFile);
        workbook.write(fos);
        fos.close();
        workbook.close();
    }

    // ✅ Get the path where the file is saved
    public String getSavedFilePath() {
        return context.getFilesDir() + "/" + OUTPUT_FILE_NAME;
    }
}
