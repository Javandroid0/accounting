package com.javandroid.accounting_app.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.javandroid.accounting_app.model.Product;
import com.javandroid.accounting_app.service.excel.ProductExcelManager;
import java.io.IOException;
import java.util.List;

public class ProductViewModel extends AndroidViewModel {
    private final ProductExcelManager productExcelManager;
    private final MutableLiveData<List<Product>> productListLiveData = new MutableLiveData<>();

    public ProductViewModel(@NonNull Application application) {
        super(application);
        productExcelManager = new ProductExcelManager(application);
        loadProducts();
    }

    public LiveData<List<Product>> getProducts() {
        return productListLiveData;
    }

    public void loadProducts() {
        try {
            List<Product> products = productExcelManager.readProducts();
            productListLiveData.setValue(products);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addProduct(Product product) {
        try {
            productExcelManager.addProduct(product);
            loadProducts();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateProduct(Product product) {
        try {
            if (productExcelManager.updateProduct(product)) {
                loadProducts();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteProduct(int id) {
        try {
            if (productExcelManager.deleteProduct(id)) {
                loadProducts();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
