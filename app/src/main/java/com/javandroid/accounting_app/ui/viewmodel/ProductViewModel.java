package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.javandroid.accounting_app.data.model.ProductEntity;
import com.javandroid.accounting_app.data.repository.ProductRepository;

import java.util.List;

public class ProductViewModel extends AndroidViewModel {

    private final ProductRepository productRepository;
    private final LiveData<List<ProductEntity>> allProducts;
    private final MutableLiveData<ProductEntity> selectedProduct = new MutableLiveData<>();

    public ProductViewModel(@NonNull Application application) {
        super(application);
        productRepository = new ProductRepository(application);
        allProducts = productRepository.getAllProducts();
    }

    // Method to fetch all products from the repository
    public LiveData<List<ProductEntity>> getAllProducts() {
        return allProducts;
    }

    // Method to add a product to the order (this can be invoked from the UI layer)
    public void addProductToOrder(ProductEntity product) {
        // Here you can add logic to add the product to the current order
        Log.d("ProductViewModel", "Product added to order: " + product.getName());
    }

    // Method to insert a new product into the database
    public void insertProduct(ProductEntity product) {
        productRepository.insert(product);
    }

    public LiveData<ProductEntity> getProductByBarcode(String barcode) {
        return productRepository.getProductByBarcode(barcode);
    }

    public void deleteProduct(ProductEntity product) {
        productRepository.delete(product);
    }

    public void updateProducts(List<ProductEntity> products) {
        productRepository.update(products);
    }

    public void updateProduct(ProductEntity product) {
        productRepository.update(product);
    }

    public ProductEntity createProduct(String name, String barcode, double buyPrice, double sellPrice, int stock) {
        ProductEntity product = new ProductEntity(name, barcode);
        product.setBuyPrice(buyPrice);
        product.setSellPrice(sellPrice);
        product.setStock(stock);
        return product;
    }

    public void insert(ProductEntity product) {
        productRepository.insert(product);
    }

    public void update(ProductEntity product) {
        productRepository.update(product);
    }

    public void delete(ProductEntity product) {
        productRepository.delete(product);
    }

    public ProductEntity getProductByBarcodeSync(String barcode) {
        return productRepository.getProductByBarcodeSync(barcode);
    }

    public ProductEntity getProductByIdSync(long productId) {
        return productRepository.getProductByIdSync(productId);
    }

    public LiveData<ProductEntity> getProductById(long productId) {
        return productRepository.getProductById(productId);
    }

    public void selectProduct(ProductEntity product) {
        selectedProduct.setValue(product);
    }

    public LiveData<ProductEntity> getSelectedProduct() {
        return selectedProduct;
    }
}
