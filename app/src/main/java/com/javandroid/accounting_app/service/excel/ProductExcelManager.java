package com.javandroid.accounting_app.service.excel;

import android.content.Context;
import android.util.Log;
import com.javandroid.accounting_app.model.Product;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ProductExcelManager {

    private static final String FILE_NAME = "product.xlsx";
    private final Context context;

    public ProductExcelManager(Context context) {
        this.context = context;
    }

    private File getExcelFile() {
        return new File(context.getFilesDir(), FILE_NAME);
    }

    // ✅ Read products from Excel
    public List<Product> readProducts() throws IOException {
        List<Product> products = new ArrayList<>();
        File excelFile = getExcelFile();
        System.out.println(excelFile);
        if (!excelFile.exists()) {
            return products; // Return empty list if file doesn't exist
        }

        FileInputStream fis = new FileInputStream(excelFile);
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheetAt(0);

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;  // Skip header row

            int id = (int) row.getCell(0).getNumericCellValue();
            String barcode = row.getCell(1).getStringCellValue();
            String name = row.getCell(2).getStringCellValue();
            double sellPrice = row.getCell(3).getNumericCellValue();
            double buyPrice = row.getCell(4).getNumericCellValue();
            System.out.println(name);
            products.add(new Product(id, barcode, name, sellPrice, buyPrice));
        }

        workbook.close();
        fis.close();
        return products;
    }

    // ✅ Write (Add new product)
    public void addProduct(Product product) throws IOException {
        File excelFile = getExcelFile();
        Workbook workbook;
        Sheet sheet;

        if (excelFile.exists()) {
            FileInputStream fis = new FileInputStream(excelFile);
            workbook = new XSSFWorkbook(fis);
            sheet = workbook.getSheetAt(0);
            fis.close();
        } else {
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("Products");

            // Create header if new file
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Barcode");
            header.createCell(2).setCellValue("Name");
            header.createCell(3).setCellValue("Sell Price");
            header.createCell(4).setCellValue("Buy Price");
        }

        int lastRow = sheet.getLastRowNum();
        Row row = sheet.createRow(lastRow + 1);
        row.createCell(0).setCellValue(product.getId());
        row.createCell(1).setCellValue(product.getBarcode());
        row.createCell(2).setCellValue(product.getName());
        row.createCell(3).setCellValue(product.getSellPrice());
        row.createCell(4).setCellValue(product.getBuyPrice());

        FileOutputStream fos = new FileOutputStream(excelFile);
        workbook.write(fos);
        fos.close();
        workbook.close();
    }

    // ✅ Update Product
    public boolean updateProduct(Product updatedProduct) throws IOException {
        File excelFile = getExcelFile();
        if (!excelFile.exists()) return false;

        FileInputStream fis = new FileInputStream(excelFile);
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheetAt(0);
        fis.close();

        boolean updated = false;

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // Skip header row
            int id = (int) row.getCell(0).getNumericCellValue();
            if (id == updatedProduct.getId()) {
                row.getCell(1).setCellValue(updatedProduct.getBarcode());
                row.getCell(2).setCellValue(updatedProduct.getName());
                row.getCell(3).setCellValue(updatedProduct.getSellPrice());
                row.getCell(4).setCellValue(updatedProduct.getBuyPrice());
                updated = true;
                break;
            }
        }

        if (updated) {
            FileOutputStream fos = new FileOutputStream(excelFile);
            workbook.write(fos);
            fos.close();
        }

        workbook.close();
        return updated;
    }

    // ✅ Delete Product by ID
    public boolean deleteProduct(int productId) throws IOException {
        File excelFile = getExcelFile();
        if (!excelFile.exists()) return false;

        FileInputStream fis = new FileInputStream(excelFile);
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheetAt(0);
        fis.close();

        boolean deleted = false;
        List<Row> rowsToRemove = new ArrayList<>();

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // Skip header row
            int id = (int) row.getCell(0).getNumericCellValue();
            if (id == productId) {
                rowsToRemove.add(row);
                deleted = true;
            }
        }

        for (Row row : rowsToRemove) {
            sheet.removeRow(row);
        }

        // Shift remaining rows up
        int rowIndex = 1;
        for (Row row : sheet) {
            if (row.getRowNum() > 0) {
                row.getSheet().shiftRows(row.getRowNum(), row.getRowNum() + 1, -1);
            }
        }

        if (deleted) {
            FileOutputStream fos = new FileOutputStream(excelFile);
            workbook.write(fos);
            fos.close();
        }

        workbook.close();
        return deleted;
    }
}
