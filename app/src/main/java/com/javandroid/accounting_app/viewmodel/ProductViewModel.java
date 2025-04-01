package com.javandroid.accounting_app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.javandroid.accounting_app.model.Product;
import java.util.List;

public class ProductViewModel extends ViewModel {
    private final MutableLiveData<List<Product>> products = new MutableLiveData<>();

    public LiveData<List<Product>> getProducts() {
        return products;
    }

    public void loadProducts(List<Product> productList) {
        products.setValue(productList);
    }
}
