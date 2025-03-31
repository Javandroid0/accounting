package com.javandroid.accounting_app.service;

import android.content.Context;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.javandroid.accounting_app.model.Order;
import com.javandroid.accounting_app.model.Product;
public class ExcelManager {
    private File file;

//    public ExcelManager(String filePath) {
//        this.file = new File(filePath);
//        initializeFile();
//    }
    public ExcelManager(Context context) {
        this.file = new File(context.getFilesDir(), "poducts.xlsx");
        System.out.println(this.file);
        initializeFile();
    }

    private void initializeFile() {
        if (!file.exists()) {
            try (Workbook workbook = new XSSFWorkbook()) {
                // Creating Product Sheet
                Sheet productSheet = workbook.createSheet("Products");
                Row productHeader = productSheet.createRow(0);
                productHeader.createCell(0).setCellValue("ID");
                productHeader.createCell(1).setCellValue("Name");
                productHeader.createCell(2).setCellValue("Barcode");
                productHeader.createCell(3).setCellValue("Sell Price");
                productHeader.createCell(4).setCellValue("Buy Price");

                // Creating Order Sheet
                Sheet orderSheet = workbook.createSheet("Orders");
                Row orderHeader = orderSheet.createRow(0);
                orderHeader.createCell(0).setCellValue("Order ID");
                orderHeader.createCell(1).setCellValue("User ID");
                orderHeader.createCell(2).setCellValue("Type");
                orderHeader.createCell(3).setCellValue("Product ID");
                orderHeader.createCell(4).setCellValue("Product Name");
                orderHeader.createCell(5).setCellValue("Sell Price");
                orderHeader.createCell(6).setCellValue("Buy Price");
                orderHeader.createCell(7).setCellValue("Quantity");

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    workbook.write(fos);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ================================
    // PRODUCT MANAGEMENT (CRUD)
    // ================================

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet("Products");
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header
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

    public void addOrUpdateProduct(Product product) {
        List<Product> products = getAllProducts();
        boolean updated = false;

        for (Product p : products) {
            if (p.getId() == product.getId()) {
                p.setName(product.getName());
                p.setBarcode(product.getBarcode());
                p.setSellPrice(product.getSellPrice());
                p.setBuyPrice(product.getBuyPrice());
                updated = true;
                break;
            }
        }

        if (!updated) {
            products.add(product);
        }

        writeProductsToExcel(products);
    }

    public void deleteProduct(int productId) {
        List<Product> products = getAllProducts();
        products.removeIf(p -> p.getId() == productId);
        writeProductsToExcel(products);
    }

    private void writeProductsToExcel(List<Product> products) {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(file)) {

            Sheet sheet = workbook.createSheet("Products");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Name");
            header.createCell(2).setCellValue("Barcode");
            header.createCell(3).setCellValue("Sell Price");
            header.createCell(4).setCellValue("Buy Price");

            int rowIndex = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(product.getId());
                row.createCell(1).setCellValue(product.getName());
                row.createCell(2).setCellValue(product.getBarcode());
                row.createCell(3).setCellValue(product.getSellPrice());
                row.createCell(4).setCellValue(product.getBuyPrice());
            }

            workbook.write(fos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================================
    // ORDER MANAGEMENT (CRUD)
    // ================================

    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet("Orders");
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                orders.add(new Order(
                        (int) row.getCell(0).getNumericCellValue(),
                        (int) row.getCell(1).getNumericCellValue(),
                        row.getCell(2).getStringCellValue(),
                        (int) row.getCell(3).getNumericCellValue(),
                        row.getCell(4).getStringCellValue(),
                        row.getCell(5).getNumericCellValue(),
                        row.getCell(6).getNumericCellValue(),
                        (int) row.getCell(7).getNumericCellValue()
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return orders;
    }

    public void addOrder(Order order) {
        List<Order> orders = getAllOrders();
        orders.add(order);
        writeOrdersToExcel(orders);
    }

    public void deleteOrder(int orderId) {
        List<Order> orders = getAllOrders();
        orders.removeIf(o -> o.getOrderId() == orderId);
        writeOrdersToExcel(orders);
    }

    private void writeOrdersToExcel(List<Order> orders) {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(file)) {

            Sheet sheet = workbook.createSheet("Orders");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Order ID");
            header.createCell(1).setCellValue("User ID");
            header.createCell(2).setCellValue("Type");
            header.createCell(3).setCellValue("Product ID");
            header.createCell(4).setCellValue("Product Name");
            header.createCell(5).setCellValue("Sell Price");
            header.createCell(6).setCellValue("Buy Price");
            header.createCell(7).setCellValue("Quantity");

            int rowIndex = 1;
            for (Order order : orders) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(order.getOrderId());
                row.createCell(1).setCellValue(order.getUserId());
                row.createCell(2).setCellValue(order.getType());
                row.createCell(3).setCellValue(order.getProductId());
                row.createCell(4).setCellValue(order.getProductName());
                row.createCell(5).setCellValue(order.getSellPrice());
                row.createCell(6).setCellValue(order.getBuyPrice());
                row.createCell(7).setCellValue(order.getQuantity());
            }

            workbook.write(fos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
