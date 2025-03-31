package com.javandroid.accounting_app.service.excel;

import android.content.Context;
import com.javandroid.accounting_app.model.Product;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ProductManager {
    private final File file;

    public ProductManager(Context context) {
        this.file = new File(context.getFilesDir(), "products.xlsx");
        initializeFile();
    }

    private void initializeFile() {
        if (!file.exists()) {
            try (Workbook workbook = new XSSFWorkbook()) {
                workbook.createSheet("Products");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    workbook.write(fos);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet("Products");
            if (sheet == null) return products;

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                products.add(new Product(
                        (int) row.getCell(0).getNumericCellValue(),
                        row.getCell(1).getStringCellValue(),
                        row.getCell(2).getStringCellValue(),
                        row.getCell(3).getNumericCellValue(),
                        row.getCell(4).getNumericCellValue()
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return products;
    }

    public void saveProducts(List<Product> products) {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(file)) {
            Sheet sheet = workbook.createSheet("Products");

            for (int i = 0; i < products.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Product p = products.get(i);
                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(p.getName());
                row.createCell(2).setCellValue(p.getBarcode());
                row.createCell(3).setCellValue(p.getSellPrice());
                row.createCell(4).setCellValue(p.getBuyPrice());
            }

            workbook.write(fos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
