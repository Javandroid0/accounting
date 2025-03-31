package com.javandroid.accounting_app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderManager extends SQLiteOpenHelper {

    private static final String TAG = "OrderManager";
    private static final String DATABASE_NAME = "AccountingApp.db";
    private static final int DATABASE_VERSION = 1;

    // Orders Table
    public static final String TABLE_ORDERS = "orders";
    public static final String COLUMN_ORDER_ID = "order_id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_ORDER_TYPE = "type";
    public static final String COLUMN_ORDER_DATE = "order_date"; // Added order date

    // Order Items Table
    public static final String TABLE_ORDER_ITEMS = "order_items";
    public static final String COLUMN_ORDER_ITEM_ID = "id";
    public static final String COLUMN_ORDER_ITEM_ORDER_ID = "order_id";
    public static final String COLUMN_ORDER_ITEM_PRODUCT_ID = "product_id";
    public static final String COLUMN_ORDER_ITEM_PRODUCT_NAME = "product_name";
    public static final String COLUMN_ORDER_ITEM_PRODUCT_SELL_PRICE = "product_sell_price";
    public static final String COLUMN_ORDER_ITEM_PRODUCT_BUY_PRICE = "product_buy_price";
    public static final String COLUMN_ORDER_ITEM_QUANTITY = "quantity";

    // SQL to create the orders table
    private static final String CREATE_ORDERS_TABLE =
            "CREATE TABLE " + TABLE_ORDERS + "("
                    + COLUMN_ORDER_ID + " TEXT PRIMARY KEY,"
                    + COLUMN_USER_ID + " TEXT,"
                    + COLUMN_ORDER_TYPE + " TEXT NOT NULL,"
                    + COLUMN_ORDER_DATE + " INTEGER NOT NULL" // Store as Unix timestamp (milliseconds)
                    + ")";

    // SQL to create the order items table
    private static final String CREATE_ORDER_ITEMS_TABLE =
            "CREATE TABLE " + TABLE_ORDER_ITEMS + "("
                    + COLUMN_ORDER_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_ORDER_ITEM_ORDER_ID + " TEXT NOT NULL,"
                    + COLUMN_ORDER_ITEM_PRODUCT_ID + " INTEGER NOT NULL,"
                    + COLUMN_ORDER_ITEM_PRODUCT_NAME + " TEXT NOT NULL,"
                    + COLUMN_ORDER_ITEM_PRODUCT_SELL_PRICE + " REAL NOT NULL,"
                    + COLUMN_ORDER_ITEM_PRODUCT_BUY_PRICE + " REAL NOT NULL,"
                    + COLUMN_ORDER_ITEM_QUANTITY + " INTEGER NOT NULL,"
                    + "FOREIGN KEY(" + COLUMN_ORDER_ITEM_ORDER_ID + ") REFERENCES " + TABLE_ORDERS + "(" + COLUMN_ORDER_ID + ") ON DELETE CASCADE," // Added ON DELETE CASCADE
                    + "FOREIGN KEY(" + COLUMN_ORDER_ITEM_PRODUCT_ID + ") REFERENCES " + ProductManager.TABLE_PRODUCTS + "(" + ProductManager.COLUMN_PRODUCT_ID + ")"
                    + ")";

    public OrderManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d(TAG, "OrderManager initialized");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_ORDERS_TABLE);
        Log.i(TAG, "Orders table created");
        db.execSQL(CREATE_ORDER_ITEMS_TABLE);
        Log.i(TAG, "Order Items table created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDER_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDERS);
        onCreate(db);
    }

    // Generate a unique order ID
    private String generateUniqueOrderId() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }

    // Create a new order and return its ID
    public String createNewOrder(String userId, String type) {
        SQLiteDatabase db = this.getWritableDatabase();
        String orderId = generateUniqueOrderId();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ORDER_ID, orderId);
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_ORDER_TYPE, type);
        values.put(COLUMN_ORDER_DATE, System.currentTimeMillis()); // Set current timestamp

        long result = db.insert(TABLE_ORDERS, null, values);
        db.close();

        if (result != -1) {
            Log.i(TAG, "New order created with ID: " + orderId);
            return orderId;
        } else {
            Log.e(TAG, "Error creating new order");
            return null;
        }
    }

    // Add an item to an existing order
    public long addOrderItem(String orderId, OrderItem orderItem) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ORDER_ITEM_ORDER_ID, orderId);
        values.put(COLUMN_ORDER_ITEM_PRODUCT_ID, orderItem.getProductId());
        values.put(COLUMN_ORDER_ITEM_PRODUCT_NAME, orderItem.getProductName());
        values.put(COLUMN_ORDER_ITEM_PRODUCT_SELL_PRICE, orderItem.getProductSellPrice());
        values.put(COLUMN_ORDER_ITEM_PRODUCT_BUY_PRICE, orderItem.getProductBuyPrice());
        values.put(COLUMN_ORDER_ITEM_QUANTITY, orderItem.getQuantity());

        long id = db.insert(TABLE_ORDER_ITEMS, null, values);
        db.close();
        Log.i(TAG, "Order item added to order " + orderId + ", product ID: " + orderItem.getProductId() + " (ID: " + id + ")");
        return id;
    }

    // Save an order with its items
    public boolean saveOrder(Order order) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // 1. Create the main order record if it doesn't exist
            if (getOrderById(order.getOrderId()) == null) {
                ContentValues orderValues = new ContentValues();
                orderValues.put(COLUMN_ORDER_ID, order.getOrderId());
                orderValues.put(COLUMN_USER_ID, order.getUserId());
                orderValues.put(COLUMN_ORDER_TYPE, order.getType());
                orderValues.put(COLUMN_ORDER_DATE, System.currentTimeMillis()); // Set current timestamp
                long orderInsertResult = db.insert(TABLE_ORDERS, null, orderValues);

                if (orderInsertResult == -1) {
                    db.setTransactionSuccessful();
                    Log.e(TAG, "Error creating main order record with ID: " + order.getOrderId());
                    return false;
                }
                Log.i(TAG, "Main order record created with ID: " + order.getOrderId());
            }

            // 2. Clear existing items for this order (in case of update)
            int deletedItems = db.delete(TABLE_ORDER_ITEMS, COLUMN_ORDER_ITEM_ORDER_ID + "=?", new String[]{order.getOrderId()});
            Log.d(TAG, "Cleared " + deletedItems + " items for order ID: " + order.getOrderId());

            // 3. Add each item to the order items table
            for (OrderItem item : order.getItems()) {
                ContentValues itemValues = new ContentValues();
                itemValues.put(COLUMN_ORDER_ITEM_ORDER_ID, order.getOrderId());
                itemValues.put(COLUMN_ORDER_ITEM_PRODUCT_ID, item.getProductId());
                itemValues.put(COLUMN_ORDER_ITEM_PRODUCT_NAME, item.getProductName());
                itemValues.put(COLUMN_ORDER_ITEM_PRODUCT_SELL_PRICE, item.getProductSellPrice());
                itemValues.put(COLUMN_ORDER_ITEM_PRODUCT_BUY_PRICE, item.getProductBuyPrice());
                itemValues.put(COLUMN_ORDER_ITEM_QUANTITY, item.getQuantity());
                long itemInsertResult = db.insert(TABLE_ORDER_ITEMS, null, itemValues);

                if (itemInsertResult == -1) {
                    db.setTransactionSuccessful();
                    Log.e(TAG, "Error adding item to order " + order.getOrderId() + ", product ID: " + item.getProductId());
                    return false;
                }
                Log.d(TAG, "Added item to order " + order.getOrderId() + ", product ID: " + item.getProductId());
            }

            db.setTransactionSuccessful();
            Log.i(TAG, "Order saved successfully with ID: " + order.getOrderId());
            return true;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // Retrieve an order by its ID (including its items)
    public Order getOrderById(String orderId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Order order = null;
        Cursor orderCursor = null;

        try {
            // Retrieve the main order details
            orderCursor = db.query(TABLE_ORDERS,
                    new String[]{COLUMN_ORDER_ID, COLUMN_USER_ID, COLUMN_ORDER_TYPE, COLUMN_ORDER_DATE},
                    COLUMN_ORDER_ID + "=?",
                    new String[]{orderId}, null, null, null);

            if (orderCursor != null && orderCursor.moveToFirst()) {
                order = cursorToOrder(orderCursor);
                if (order != null) {
                    // Retrieve the items for this order
                    List<OrderItem> orderItems = getOrderItemsByOrderId(orderId);
                    order.setItems(orderItems);
                    Log.d(TAG, "Retrieved order with ID: " + orderId + " with " + orderItems.size() + " items.");
                } else {
                    Log.e(TAG, "Error creating Order object from cursor for ID: " + orderId);
                }
            } else {
                Log.d(TAG, "Order with ID: " + orderId + " not found.");
            }
        } finally {
            if (orderCursor != null) {
                orderCursor.close();
            }
            db.close();
        }
        return order;
    }

    // Helper method to retrieve order items for a given order ID
    private List<OrderItem> getOrderItemsByOrderId(String orderId) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<OrderItem> orderItems = new ArrayList<>();
        Cursor itemsCursor = null;

        try {
            itemsCursor = db.query(TABLE_ORDER_ITEMS,
                    new String[]{COLUMN_ORDER_ITEM_PRODUCT_ID, COLUMN_ORDER_ITEM_PRODUCT_NAME, COLUMN_ORDER_ITEM_PRODUCT_SELL_PRICE, COLUMN_ORDER_ITEM_PRODUCT_BUY_PRICE, COLUMN_ORDER_ITEM_QUANTITY},
                    COLUMN_ORDER_ITEM_ORDER_ID + "=?",
                    new String[]{orderId}, null, null, null);

            if (itemsCursor != null && itemsCursor.moveToFirst()) {
                do {
                    OrderItem item = cursorToOrderItem(itemsCursor);
                    if (item != null) {
                        orderItems.add(item);
                    }
                } while (itemsCursor.moveToNext());
                Log.d(TAG, "Retrieved " + orderItems.size() + " items for order ID: " + orderId);
            } else {
                Log.d(TAG, "No items found for order ID: " + orderId);
            }
        } finally {
            if (itemsCursor != null) {
                itemsCursor.close();
            }
        }
        return orderItems;
    }

    // Retrieve all orders (you might want to add filtering/pagination later)
    public List<Order> getAllOrders() {
        List<Order> orderList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_ORDERS;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Order order = cursorToOrder(cursor);
                    if (order != null) {
                        // Retrieve items for each order
                        List<OrderItem> items = getOrderItemsByOrderId(order.getOrderId());
                        order.setItems(items);
                        orderList.add(order);
                    } else {
                        Log.e(TAG, "Error creating Order object from cursor in getAllOrders.");
                    }
                } while (cursor.moveToNext());
                Log.d(TAG, "Retrieved " + orderList.size() + " orders.");
            } else {
                Log.d(TAG, "No orders found.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return orderList;
    }

    // Helper method to convert a Cursor row to an Order object
    private Order cursorToOrder(Cursor cursor) {
        int orderIdIndex = cursor.getColumnIndex(COLUMN_ORDER_ID);
        int userIdIndex = cursor.getColumnIndex(COLUMN_USER_ID);
        int orderTypeIndex = cursor.getColumnIndex(COLUMN_ORDER_TYPE);
        int orderDateIndex = cursor.getColumnIndex(COLUMN_ORDER_DATE);

        if (orderIdIndex != -1 && userIdIndex != -1 && orderTypeIndex != -1 && orderDateIndex != -1) {
            Order order = new Order();
            order.setOrderId(cursor.getString(orderIdIndex));
            order.setUserId(cursor.getString(userIdIndex));
            order.setType(cursor.getString(orderTypeIndex));
            order.setOrderDate(cursor.getLong(orderDateIndex));
            return order;
        } else {
            Log.e(TAG, "Error creating Order object from Cursor: One or more columns not found in orders table.");
            return null;
        }
    }

    // Helper method to convert a Cursor row to an OrderItem object
    private OrderItem cursorToOrderItem(Cursor cursor) {
        int productIdIndex = cursor.getColumnIndex(COLUMN_ORDER_ITEM_PRODUCT_ID);
        int productNameIndex = cursor.getColumnIndex(COLUMN_ORDER_ITEM_PRODUCT_NAME);
        int productSellPriceIndex = cursor.getColumnIndex(COLUMN_ORDER_ITEM_PRODUCT_SELL_PRICE);
        int productBuyPriceIndex = cursor.getColumnIndex(COLUMN_ORDER_ITEM_PRODUCT_BUY_PRICE);
        int quantityIndex = cursor.getColumnIndex(COLUMN_ORDER_ITEM_QUANTITY);

        if (productIdIndex != -1 && productNameIndex != -1 && productSellPriceIndex != -1 && productBuyPriceIndex != -1 && quantityIndex != -1) {
            return new OrderItem(
                    cursor.getInt(productIdIndex),
                    cursor.getString(productNameIndex),
                    cursor.getDouble(productSellPriceIndex),
                    cursor.getDouble(productBuyPriceIndex),
                    cursor.getInt(quantityIndex)
            );
        } else {
            Log.e(TAG, "Error creating OrderItem from Cursor: One or more columns not found in order_items table.");
            return null;
        }
    }

    // Delete a specific order and its items
    public boolean deleteOrder(String orderId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Delete order items first (due to foreign key constraint with ON DELETE CASCADE)
            int itemsDeleted = db.delete(TABLE_ORDER_ITEMS, COLUMN_ORDER_ITEM_ORDER_ID + "=?", new String[]{orderId});
            Log.d(TAG, "Deleted " + itemsDeleted + " items for order ID: " + orderId);

            // Then delete the main order
            int ordersDeleted = db.delete(TABLE_ORDERS, COLUMN_ORDER_ID + "=?", new String[]{orderId});

            db.setTransactionSuccessful();
            Log.i(TAG, "Deleted order with ID: " + orderId + " (and " + itemsDeleted + " items).");
            return ordersDeleted > 0;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // You might want to add methods for querying orders by date range, user, etc.
}