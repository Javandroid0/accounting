package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;

//import androidx.annotation.NonNull;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.room.Room;

import com.javandroid.accounting_app.data.database.AppDatabase;
import com.javandroid.accounting_app.data.model.Order;
import com.javandroid.accounting_app.data.model.Product;

import java.util.List;

public class OrderViewModel extends AndroidViewModel {
    private AppDatabase appDatabase;
    private MutableLiveData<List<Order>> ordersLiveData = new MutableLiveData<>();

    public OrderViewModel(@NonNull Application application) {
        super(application);
        appDatabase = Room.databaseBuilder(application, AppDatabase.class, "shop-db").build();

    }
    private void loadOrders() {
        new Thread(() -> {
            List<Order> orders = appDatabase.orderDao().getAllOrders();
            ordersLiveData.postValue(orders);
        }).start();
    }
    public void insertProduct(Product product) {
        new Thread(() -> appDatabase.productDao().insert(product)).start();
    }


    public void scanBarcode(String barcode) {
        new Thread(() -> {
//            System.out.println(barcode);

            Product product = new Product();
            product.setName("Coca-Cola");
            product.setBarcode("8702334854112");
            product.setSellPrice(2.50);
            product.setBuyPrice(1.75);
            insertProduct(product);

//            Product product = appDatabase.productDao().getProductByBarcode(barcode);
//            System.out.println(product.getBarcode());
            if (product != null) {
                Order order = new Order();
                order.setOrderId(product.getId());
                order.setProductName(product.getName());
                order.setProductId(product.getBarcode());
                order.setProductSellPrice(product.getSellPrice());
                order.setProductBuyPrice(product.getBuyPrice());
                order.setQuantity(1);
                appDatabase.orderDao().insert(order);
                loadOrders();
            }
        }).start();
    }

    public void addOrder(Order order) {
        // You can move this to a background thread using AsyncTask or coroutines (if Kotlin)
        new Thread(() -> appDatabase.orderDao().insert(order)).start();
    }

    public LiveData<List<Order>> getOrders() {
        return ordersLiveData;
    }

//    public void scanBarcode(String barcode) {
//        new Thread(() -> {
//            Product product = appDatabase.productDao().getProductByBarcode(barcode);
//            if (product != null) {
//                // Logic to add the product to the current order
//            }
//        }).start();
//    }
}

