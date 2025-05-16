package com.javandroid.accounting_app.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.javandroid.accounting_app.data.dao.CustomerDao;
import com.javandroid.accounting_app.data.dao.OrderDao;
import com.javandroid.accounting_app.data.dao.ProductDao;
import com.javandroid.accounting_app.data.dao.UserDao;
import com.javandroid.accounting_app.data.model.CustomerEntity;
import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.model.ProductEntity;
import com.javandroid.accounting_app.data.model.UserEntity;

@Database(entities = {
        ProductEntity.class,
        OrderEntity.class,
        OrderItemEntity.class,
        UserEntity.class,
        CustomerEntity.class
}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract ProductDao productDao();

    public abstract OrderDao orderDao();

    public abstract UserDao userDao();

    public abstract CustomerDao customerDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "shop-db")
                            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                            .build();
                }
            }
        }
        return instance;
    }

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Create new tables with the new schema
            database.execSQL("CREATE TABLE IF NOT EXISTS products_new (" +
                    "productId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "barcode TEXT NOT NULL, " +
                    "buyPrice REAL NOT NULL DEFAULT 0, " +
                    "sellPrice REAL NOT NULL DEFAULT 0, " +
                    "stock INTEGER NOT NULL DEFAULT 0)");

            // Copy data from old table
            database.execSQL("INSERT INTO products_new (productId, name, barcode, buyPrice, sellPrice) " +
                    "SELECT id, name, barcode, buyPrice, sellPrice FROM products");

            // Drop old table
            database.execSQL("DROP TABLE products");

            // Rename new table to original name
            database.execSQL("ALTER TABLE products_new RENAME TO products");

            // Create orders table
            database.execSQL("CREATE TABLE IF NOT EXISTS orders (" +
                    "orderId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "date TEXT NOT NULL, " +
                    "total REAL NOT NULL DEFAULT 0, " +
                    "customerId INTEGER NOT NULL, " +
                    "userId INTEGER NOT NULL, " +
                    "FOREIGN KEY (customerId) REFERENCES customers(customerId) ON DELETE CASCADE, " +
                    "FOREIGN KEY (userId) REFERENCES users(userId) ON DELETE CASCADE)");

            // Create order_items table
            database.execSQL("CREATE TABLE IF NOT EXISTS order_items (" +
                    "itemId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "orderId INTEGER, " +
                    "productId INTEGER, " +
                    "productName TEXT NOT NULL, " +
                    "barcode TEXT NOT NULL, " +
                    "buyPrice REAL NOT NULL DEFAULT 0, " +
                    "sellPrice REAL NOT NULL DEFAULT 0, " +
                    "quantity REAL NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY (orderId) REFERENCES orders(orderId) ON DELETE CASCADE, " +
                    "FOREIGN KEY (productId) REFERENCES products(productId) ON DELETE SET NULL)");

            // Create indices for better query performance
            database.execSQL("CREATE INDEX IF NOT EXISTS index_order_items_orderId ON order_items(orderId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_order_items_productId ON order_items(productId)");
        }
    };

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Recreate the orders table with correct column names
            database.execSQL("DROP TABLE IF EXISTS order_items");
            database.execSQL("DROP TABLE IF EXISTS orders");

            // Create orders table with correct column names
            database.execSQL("CREATE TABLE IF NOT EXISTS orders (" +
                    "orderId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "date TEXT NOT NULL, " +
                    "total REAL NOT NULL DEFAULT 0, " +
                    "customerId INTEGER NOT NULL, " +
                    "userId INTEGER NOT NULL, " +
                    "FOREIGN KEY (customerId) REFERENCES customers(customerId) ON DELETE CASCADE, " +
                    "FOREIGN KEY (userId) REFERENCES users(userId) ON DELETE CASCADE)");

            // Create order_items table with correct reference
            database.execSQL("CREATE TABLE IF NOT EXISTS order_items (" +
                    "itemId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "orderId INTEGER, " +
                    "productId INTEGER, " +
                    "productName TEXT NOT NULL, " +
                    "barcode TEXT NOT NULL, " +
                    "buyPrice REAL NOT NULL DEFAULT 0, " +
                    "sellPrice REAL NOT NULL DEFAULT 0, " +
                    "quantity REAL NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY (orderId) REFERENCES orders(orderId) ON DELETE CASCADE, " +
                    "FOREIGN KEY (productId) REFERENCES products(productId) ON DELETE SET NULL)");

            // Create indices for better query performance
            database.execSQL("CREATE INDEX IF NOT EXISTS index_order_items_orderId ON order_items(orderId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_order_items_productId ON order_items(productId)");
        }
    };

}
