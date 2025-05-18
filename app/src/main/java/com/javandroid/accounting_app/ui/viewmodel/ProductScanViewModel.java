package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.javandroid.accounting_app.data.model.ProductEntity;
import com.javandroid.accounting_app.data.repository.OrderStateRepository;
import com.javandroid.accounting_app.data.repository.OrderSessionManager;
import com.javandroid.accounting_app.data.repository.ProductRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel responsible for handling barcode scanning and product operations
 */
public class ProductScanViewModel extends AndroidViewModel {
    private static final String TAG = "ProductScanViewModel";

    private final ProductRepository productRepository;
    private final OrderStateRepository stateRepository;
    private final CurrentOrderViewModel currentOrderViewModel;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Event to signal when product is not found
    private final MutableLiveData<String> productNotFoundEvent = new MutableLiveData<>();

    // Scanner active state - used to control the barcode scanner's active state
    private final MutableLiveData<Boolean> scannerActiveState = new MutableLiveData<>(true);

    // Event for product operation results
    private final MutableLiveData<ProductOperationMessage> productOperationMessage = new MutableLiveData<>();

    // Event types for product operations
    public enum ProductOperationResult {
        ADDED_SUCCESSFULLY,
        OUT_OF_STOCK,
        NOT_FOUND,
        ERROR
    }

    // Message class for product operations
    public static class ProductOperationMessage {
        private final ProductOperationResult result;
        private final String message;
        private final ProductEntity product;

        public ProductOperationMessage(ProductOperationResult result, String message, ProductEntity product) {
            this.result = result;
            this.message = message;
            this.product = product;
        }

        public ProductOperationResult getResult() {
            return result;
        }

        public String getMessage() {
            return message;
        }

        public ProductEntity getProduct() {
            return product;
        }
    }

    public ProductScanViewModel(@NonNull Application application) {
        super(application);
        productRepository = new ProductRepository(application);
        stateRepository = OrderSessionManager.getInstance().getCurrentRepository();
        currentOrderViewModel = new ViewModelProvider.AndroidViewModelFactory(application)
                .create(CurrentOrderViewModel.class);
    }

    /**
     * Add a product to the order by its barcode
     */
    public void addProductByBarcode(String barcode, double quantity) {
        if (barcode == null || barcode.isEmpty()) {
            return;
        }

        executor.execute(() -> {
            try {
                // Get product by barcode
                ProductEntity product = productRepository.getProductByBarcodeSync(barcode);

                if (product != null) {
                    // Check if there's enough stock
                    if (product.getStock() >= quantity) {
                        // Product found with stock, add to order on main thread
                        new Handler(getApplication().getMainLooper()).post(() -> {
                            currentOrderViewModel.addProduct(product, quantity);

                            // Notify UI of success
                            productOperationMessage.setValue(
                                    new ProductOperationMessage(
                                            ProductOperationResult.ADDED_SUCCESSFULLY,
                                            "Product added: " + product.getName(),
                                            product));
                        });
                    } else {
                        // Not enough stock
                        productOperationMessage.postValue(
                                new ProductOperationMessage(
                                        ProductOperationResult.OUT_OF_STOCK,
                                        "Product out of stock: " + product.getName(),
                                        product));
                    }
                } else {
                    // Product not found, trigger event for UI to handle
                    productNotFoundEvent.postValue(barcode);
                    productOperationMessage.postValue(
                            new ProductOperationMessage(
                                    ProductOperationResult.NOT_FOUND,
                                    "Product not found for barcode: " + barcode,
                                    null));
                }
            } catch (Exception e) {
                // Handle errors
                productOperationMessage.postValue(
                        new ProductOperationMessage(
                                ProductOperationResult.ERROR,
                                "Error processing barcode: " + e.getMessage(),
                                null));
            }
        });
    }

    /**
     * Cancel the product add flow and return to scanning
     * Called when user chooses not to add a product that was not found
     */
    public void cancelProductAddFlow() {
        Log.d(TAG, "Product add flow canceled by user");
        // Clear any related UI state
        productNotFoundEvent.postValue(null);

        // Return to scanner or previous screen by activating the scanner
        scannerActiveState.postValue(true);
    }

    /**
     * Get the scanner active state LiveData
     */
    public LiveData<Boolean> getScannerActiveState() {
        return scannerActiveState;
    }

    /**
     * Set the scanner active state
     */
    public void setScannerActive(boolean active) {
        scannerActiveState.postValue(active);
    }

    /**
     * Get the event for product not found
     */
    public LiveData<String> getProductNotFoundEvent() {
        return productNotFoundEvent;
    }

    /**
     * Get the event for product operation message
     */
    public LiveData<ProductOperationMessage> getProductOperationMessage() {
        return productOperationMessage;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Shut down the executor when ViewModel is cleared
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }
}