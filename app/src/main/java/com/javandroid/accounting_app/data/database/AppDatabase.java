package com.javandroid.accounting_app.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.javandroid.accounting_app.data.dao.OrderDao;
import com.javandroid.accounting_app.data.dao.ProductDao;
import com.javandroid.accounting_app.data.dao.UserDao;
import com.javandroid.accounting_app.data.model.Order;
import com.javandroid.accounting_app.data.model.Product;
import com.javandroid.accounting_app.data.model.User;

@Database(entities = {Product.class, Order.class, User.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract ProductDao productDao();
    public abstract OrderDao orderDao();
    public abstract UserDao userDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
//                    instance = Room.databaseBuilder(
//                            context.getApplicationContext(),
//                            AppDatabase.class,
//                            "shop-db"
//                    ).fallbackToDestructiveMigration().build();
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "shop-db"
                            ).addMigrations(MIGRATION_2_3) // attach the migration here
                            .build();

                }
            }
        }
        return instance;
    }

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add the new column with a default value
            database.execSQL("ALTER TABLE Product ADD COLUMN description TEXT");
        }
    };

}
