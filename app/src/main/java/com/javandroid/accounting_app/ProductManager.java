package com.javandroid.accounting_app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.javandroid.accounting_app.model.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductManager extends SQLiteOpenHelper {

    private static final String TAG = "ProductManager";
    private static final String DATABASE_NAME = "AccountingApp.db";
    private static final int DATABASE_VERSION = 1;

    // Products Table
    public static final String TABLE_PRODUCTS = "products";
    public static final String COLUMN_PRODUCT_ID = "id";
    public static final String COLUMN_PRODUCT_BARCODE = "barcode";
    public static final String COLUMN_PRODUCT_NAME = "name";
    public static final String COLUMN_PRODUCT_SELL_PRICE = "sell_price";
    public static final String COLUMN_PRODUCT_BUY_PRICE = "buy_price";

    // SQL to create the products table
    private static final String CREATE_PRODUCTS_TABLE =
            "CREATE TABLE " + TABLE_PRODUCTS + "("
                    + COLUMN_PRODUCT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_PRODUCT_BARCODE + " TEXT UNIQUE,"
                    + COLUMN_PRODUCT_NAME + " TEXT NOT NULL,"
                    + COLUMN_PRODUCT_SELL_PRICE + " REAL NOT NULL,"
                    + COLUMN_PRODUCT_BUY_PRICE + " REAL NOT NULL"
                    + ")";

    public ProductManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d(TAG, "ProductManager initialized");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_PRODUCTS_TABLE);
        Log.i(TAG, "Products table created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        // Handle database upgrades if needed (e.g., adding new tables or columns)
        // For a simple example, we'll just drop and recreate the table
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        onCreate(db);
    }

    // Add a new product
    public long addProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PRODUCT_BARCODE, product.getBarcode());
        values.put(COLUMN_PRODUCT_NAME, product.getName());
        values.put(COLUMN_PRODUCT_SELL_PRICE, product.getSellPrice());
        values.put(COLUMN_PRODUCT_BUY_PRICE, product.getBuyPrice());

        long id = db.insert(TABLE_PRODUCTS, null, values);
        db.close();
        Log.i(TAG, "Product added: " + product.getName() + " (ID: " + id + ")");
        return id;
    }

    // Retrieve a product by ID
    public Product getProductById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Product product = null;
        try {
            cursor = db.query(TABLE_PRODUCTS,
                    new String[]{COLUMN_PRODUCT_ID, COLUMN_PRODUCT_BARCODE, COLUMN_PRODUCT_NAME, COLUMN_PRODUCT_SELL_PRICE, COLUMN_PRODUCT_BUY_PRICE},
                    COLUMN_PRODUCT_ID + "=?",
                    new String[]{String.valueOf(id)}, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                product = cursorToProduct(cursor);
                Log.d(TAG, "Retrieved product by ID " + id + ": " + product.getName());
            } else {
                Log.d(TAG, "Product with ID " + id + " not found.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return product;
    }

    // Retrieve a product by Barcode
    public Product getProductByBarcode(String barcode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Product product = null;
        try {
            cursor = db.query(TABLE_PRODUCTS,
                    new String[]{COLUMN_PRODUCT_ID, COLUMN_PRODUCT_BARCODE, COLUMN_PRODUCT_NAME, COLUMN_PRODUCT_SELL_PRICE, COLUMN_PRODUCT_BUY_PRICE},
                    COLUMN_PRODUCT_BARCODE + "=?",
                    new String[]{barcode}, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                product = cursorToProduct(cursor);
                Log.d(TAG, "Retrieved product by barcode " + barcode + ": " + product.getName());
            } else {
                Log.d(TAG, "Product with barcode " + barcode + " not found.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return product;
    }

    // Update product information
    public int updateProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PRODUCT_BARCODE, product.getBarcode());
        values.put(COLUMN_PRODUCT_NAME, product.getName());
        values.put(COLUMN_PRODUCT_SELL_PRICE, product.getSellPrice());
        values.put(COLUMN_PRODUCT_BUY_PRICE, product.getBuyPrice());

        int rowsAffected = db.update(TABLE_PRODUCTS, values, COLUMN_PRODUCT_ID + "=?",
                new String[]{String.valueOf(product.getId())});
        db.close();
        if (rowsAffected > 0) {
            Log.i(TAG, "Product updated: " + product.getName() + " (ID: " + product.getId() + ")");
        } else {
            Log.w(TAG, "Product update failed for ID: " + product.getId() + " (Product not found or no changes)");
        }
        return rowsAffected;
    }

    // List all products
    public List<Product> getAllProducts() {
        List<Product> productList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_PRODUCTS;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Product product = cursorToProduct(cursor);
                    productList.add(product);
                } while (cursor.moveToNext());
                Log.d(TAG, "Retrieved " + productList.size() + " products.");
            } else {
                Log.d(TAG, "No products found in the database.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return productList;
    }

    // Delete a product by ID
    public boolean deleteProduct(int productId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_PRODUCTS, COLUMN_PRODUCT_ID + "=?",
                new String[]{String.valueOf(productId)});
        db.close();
        if (rowsDeleted > 0) {
            Log.i(TAG, "Product deleted with ID: " + productId);
            return true;
        } else {
            Log.w(TAG, "Product deletion failed for ID: " + productId + " (Product not found)");
            return false;
        }
    }

    // Helper method to convert a Cursor row to a Product object
    private Product cursorToProduct(Cursor cursor) {
        int idIndex = cursor.getColumnIndex(COLUMN_PRODUCT_ID);
        int barcodeIndex = cursor.getColumnIndex(COLUMN_PRODUCT_BARCODE);
        int nameIndex = cursor.getColumnIndex(COLUMN_PRODUCT_NAME);
        int sellPriceIndex = cursor.getColumnIndex(COLUMN_PRODUCT_SELL_PRICE);
        int buyPriceIndex = cursor.getColumnIndex(COLUMN_PRODUCT_BUY_PRICE);

        if (idIndex != -1 && barcodeIndex != -1 && nameIndex != -1 && sellPriceIndex != -1 && buyPriceIndex != -1) {
            Product product = new Product();
            product.setId(cursor.getInt(idIndex));
            product.setBarcode(cursor.getString(barcodeIndex));
            product.setName(cursor.getString(nameIndex));
            product.setSellPrice(cursor.getDouble(sellPriceIndex));
            product.setBuyPrice(cursor.getDouble(buyPriceIndex));
            return product;
        } else {
            Log.e(TAG, "Error creating Product object from Cursor: One or more columns not found.");
            return null; // Or throw an exception
        }
    }
}