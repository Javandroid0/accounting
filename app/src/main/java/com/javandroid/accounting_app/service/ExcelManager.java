package com.javandroid.accounting_app.service;

import com.javandroid.accounting_app.model.Order;
import com.javandroid.accounting_app.model.Product;
import com.javandroid.accounting_app.model.User;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ExcelManager {
    private String filePath;
    private Workbook workbook;
    private Sheet usersSheet;
    private Sheet productsSheet;
    private Sheet ordersSheet;

    public ExcelManager(String filePath) {
        this.filePath = filePath;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                workbook = new XSSFWorkbook();
                usersSheet = workbook.createSheet("Users");
                productsSheet = workbook.createSheet("Products");
                ordersSheet = workbook.createSheet("Orders");
                initializeSheets();
                saveFile();
            } else {
                FileInputStream fileInputStream = new FileInputStream(filePath);
                workbook = new XSSFWorkbook(fileInputStream);
                usersSheet = workbook.getSheet("Users");
                productsSheet = workbook.getSheet("Products");
                ordersSheet = workbook.getSheet("Orders");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeSheets() {
        // Add headers to sheets
        Row userHeader = usersSheet.createRow(0);
        userHeader.createCell(0).setCellValue("ID");
        userHeader.createCell(1).setCellValue("Name");
        userHeader.createCell(2).setCellValue("Role");

        Row productHeader = productsSheet.createRow(0);
        productHeader.createCell(0).setCellValue("ID");
        productHeader.createCell(1).setCellValue("Barcode");
        productHeader.createCell(2).setCellValue("Name");
        productHeader.createCell(3).setCellValue("Sell Price");
        productHeader.createCell(4).setCellValue("Buy Price");

        Row orderHeader = ordersSheet.createRow(0);
        orderHeader.createCell(0).setCellValue("Order ID");
        orderHeader.createCell(1).setCellValue("User ID");
        orderHeader.createCell(2).setCellValue("Product ID");
        orderHeader.createCell(3).setCellValue("Product Name");
        orderHeader.createCell(4).setCellValue("Sell Price");
        orderHeader.createCell(5).setCellValue("Buy Price");
        orderHeader.createCell(6).setCellValue("Quantity");
        orderHeader.createCell(7).setCellValue("Profit");
    }

    public void saveFile() {
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addUser(User user) {
        Row row = usersSheet.createRow(usersSheet.getPhysicalNumberOfRows());
        row.createCell(0).setCellValue(user.getId());
        row.createCell(1).setCellValue(user.getName());
        row.createCell(2).setCellValue(user.getRole());
        saveFile();
    }

    public void addProduct(Product product) {
        Row row = productsSheet.createRow(productsSheet.getPhysicalNumberOfRows());
        row.createCell(0).setCellValue(product.getId());
        row.createCell(1).setCellValue(product.getBarcode());
        row.createCell(2).setCellValue(product.getName());
        row.createCell(3).setCellValue(product.getSellPrice());
        row.createCell(4).setCellValue(product.getBuyPrice());
        saveFile();
    }

    public void addOrder(Order order) {
        Row row = ordersSheet.createRow(ordersSheet.getPhysicalNumberOfRows());
        row.createCell(0).setCellValue(order.getOrderId());
        row.createCell(1).setCellValue(order.getUserId());
        row.createCell(2).setCellValue(order.getProductId());
        row.createCell(3).setCellValue(order.getProductName());
        row.createCell(4).setCellValue(order.getSellPrice());
        row.createCell(5).setCellValue(order.getBuyPrice());
        row.createCell(6).setCellValue(order.getQuantity());
        row.createCell(7).setCellValue(order.calculateProfit());
        saveFile();
    }

    public double calculateTotalProfit() {
        double totalProfit = 0.0;
        for (Row row : ordersSheet) {
            if (row.getRowNum() == 0) continue; // Skip header row
            totalProfit += row.getCell(7).getNumericCellValue();
        }
        return totalProfit;
    }

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        for (Row row : productsSheet) {
            if (row.getRowNum() == 0) continue;
            int id = (int) row.getCell(0).getNumericCellValue();
            String barcode = row.getCell(1).getStringCellValue();
            String name = row.getCell(2).getStringCellValue();
            double sellPrice = row.getCell(3).getNumericCellValue();
            double buyPrice = row.getCell(4).getNumericCellValue();
            products.add(new Product(id, name, barcode, sellPrice, buyPrice));
        }
        return products;
    }
}
