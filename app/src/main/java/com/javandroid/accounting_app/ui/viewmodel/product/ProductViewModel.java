package com.javandroid.accounting_app.ui.viewmodel.product;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.javandroid.accounting_app.data.model.ProductEntity;
import com.javandroid.accounting_app.data.repository.ProductRepository;

import java.util.List;
import java.util.concurrent.ExecutorService; // Added for direct execution if needed
import java.util.concurrent.Executors;   // Added

public class ProductViewModel extends AndroidViewModel {

    private final ProductRepository productRepository;
    private final LiveData<List<ProductEntity>> allProducts;
    private final MutableLiveData<ProductEntity> selectedProduct = new MutableLiveData<>();

    private final MutableLiveData<SortType> sortOrder = new MutableLiveData<>(SortType.DEFAULT);

    private final ExecutorService executor; // For background tasks if needed directly by VM

    private final LiveData<List<ProductEntity>> products;

    public enum SortType {
        DEFAULT,
        BY_STOCK
    }

    public ProductViewModel(@NonNull Application application) {
        super(application);
        productRepository = new ProductRepository(application);
        allProducts = productRepository.getAllProducts();
        executor = Executors.newSingleThreadExecutor(); // Initialize executor
        products = Transformations.switchMap(sortOrder, sort -> {
            if (sort == SortType.BY_STOCK) {
                return productRepository.getAllProductsSortedByStock();
            } else {
                return allProducts; // The original, default-sorted list
            }
        });
    }

    // 6. Your Fragment will call this method to get the list.
//    Rename the old `getAllProducts()` to avoid confusion if you prefer,
//    or replace it with this one.
    public LiveData<List<ProductEntity>> getProducts() {
        return products;
    }

    // 7. Add a method for the Fragment to call to change the sort order
    public void changeSortOrder(SortType newSortType) {
        sortOrder.setValue(newSortType);
    }

    public LiveData<List<ProductEntity>> getAllProducts() {
        return allProducts;
    }

    public void addProductToOrder(ProductEntity product) {
        Log.d("ProductViewModel", "Product added to order: " + product.getName());
    }

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

    // This is the key method used for updating a single product, including its stock
    public void update(ProductEntity product) {
        productRepository.update(product);
    }

    // Synchronous get for use in background threads (like in OrderEditViewModel)
    public ProductEntity getProductByIdSync(long productId) {
        return productRepository.getProductByIdSync(productId);
    }

    public LiveData<ProductEntity> getProductById(long productId) {
        return productRepository.getProductById(productId);
    }

    // Add this new method to your ProductViewModel class

    public void deleteAllProducts() {
        productRepository.deleteAll();
    }

    public void selectProduct(ProductEntity product) {
        selectedProduct.setValue(product);
    }

    public LiveData<ProductEntity> getSelectedProduct() {
        return selectedProduct;
    }

    // If you need to explicitly adjust stock and want a dedicated method:
    // This requires ProductRepository to also have a synchronous update method or handle it internally.
    public void adjustProductStock(long productId, double quantityChange) {
        executor.execute(() -> {
            ProductEntity product = productRepository.getProductByIdSync(productId);
            if (product != null) {
                Log.d("ProductViewModel", "Adjusting stock for " + product.getName() +
                        ". Current: " + product.getStock() + ", Change: " + quantityChange);
                product.setStock(product.getStock() + quantityChange); // quantityChange is delta (negative to decrease)
                productRepository.update(product);
            } else {
                Log.e("ProductViewModel", "Product not found for stock adjustment: ID " + productId);
            }
        });
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}