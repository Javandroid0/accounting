package com.javandroid.accounting_app.service;

//import model.Product;
//import service.ExcelManager;
import com.javandroid.accounting_app.model.Product;

import java.util.List;
import java.util.stream.Collectors;

public class ProductController {
    private ExcelManager excelManager;

    public ProductController(ExcelManager excelManager) {
        this.excelManager = excelManager;
    }

    public List<Product> getAllProducts() {
        return excelManager.getAllProducts();
    }

    public List<Product> searchProduct(String query) {
        return getAllProducts().stream()
                .filter(product -> product.getName().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }
//
//    public void addProduct(Product product) {
//        excelManager.addProduct(product);
//    }
//
//    public void updateProduct(Product product) {
//        excelManager.updateProduct(product);
//    }

    public void deleteProduct(int productId) {
        excelManager.deleteProduct(productId);
    }
}
