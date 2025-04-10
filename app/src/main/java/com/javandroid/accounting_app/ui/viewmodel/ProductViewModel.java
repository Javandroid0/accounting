package com.javandroid.accounting_app.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.room.Room;

import com.javandroid.accounting_app.data.database.AppDatabase;
import com.javandroid.accounting_app.data.model.Product;

public class ProductViewModel extends AndroidViewModel {
    private final AppDatabase db;

    public ProductViewModel(@NonNull Application application) {
        super(application);
        db = Room.databaseBuilder(application, AppDatabase.class, "shop-db").build();
    }

    public void insertProduct(Product product) {
        new Thread(() -> db.productDao().insert(product)).start();
    }
}
