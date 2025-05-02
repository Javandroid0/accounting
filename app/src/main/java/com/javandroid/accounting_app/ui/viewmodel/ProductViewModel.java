package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.javandroid.accounting_app.data.model.Product;
import com.javandroid.accounting_app.data.repository.ProductRepository;

import java.util.List;

public class ProductViewModel extends AndroidViewModel {

    private final ProductRepository productRepository;
    private final MutableLiveData<List<Product>> productListLiveData = new MutableLiveData<>();

    public ProductViewModel(Application application) {
        super(application);
        productRepository = new ProductRepository(application);
    }

    // Method to fetch all products from the repository
    public LiveData<List<Product>> getAllProducts() {
        return productRepository.getAllProducts();
    }


    public LiveData<List<Product>> getAllProducts1() {
        return productRepository.getAllProducts();
    }

    // Method to add a product to the order (this can be invoked from the UI layer)
    public void addProductToOrder(Product product) {
        // Here you can add logic to add the product to the current order
        Log.d("ProductViewModel", "Product added to order: " + product.getName());
    }

    // Method to insert a new product into the database
    public void insertProduct(Product product) {
        productRepository.insert(product);
    }

    public LiveData<Product> getProductByBarcode(String barcode) {
        return productRepository.getProductByBarcode(barcode);
    }

    public void deleteProduct(Product product) {
        productRepository.delete(product);
    }

    public void updateProducts(List<Product> products) {
        productRepository.update(products);
    }

}
