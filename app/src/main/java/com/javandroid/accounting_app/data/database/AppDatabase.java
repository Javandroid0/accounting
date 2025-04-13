package com.javandroid.accounting_app.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.javandroid.accounting_app.data.dao.OrderDao;
import com.javandroid.accounting_app.data.dao.ProductDao;
import com.javandroid.accounting_app.data.dao.UserDao;
import com.javandroid.accounting_app.data.model.Order;
import com.javandroid.accounting_app.data.model.Product;
import com.javandroid.accounting_app.data.model.User;

@Database(entities = {Product.class, Order.class, User.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract ProductDao productDao();
    public abstract OrderDao orderDao();
    public abstract UserDao userDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "shop-db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return instance;
    }
}
