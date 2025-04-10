package com.javandroid.accounting_app.data.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.javandroid.accounting_app.data.dto.OrderDao;
import com.javandroid.accounting_app.data.dto.ProductDao;
import com.javandroid.accounting_app.data.model.Order;
import com.javandroid.accounting_app.data.model.Product;

@Database(entities = {Product.class, Order.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ProductDao productDao();
    public abstract OrderDao orderDao();
}
