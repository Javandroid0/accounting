package com.javandroid.accounting_app.data.backup;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.javandroid.accounting_app.data.database.AppDatabase;
import com.javandroid.accounting_app.data.model.OrderEntity;
import com.javandroid.accounting_app.data.model.OrderItemEntity;
import com.javandroid.accounting_app.data.model.ProductEntity;
import com.javandroid.accounting_app.data.model.CustomerEntity;
import com.javandroid.accounting_app.data.model.UserEntity;
import com.javandroid.accounting_app.data.repository.OrderRepository;
import com.javandroid.accounting_app.data.repository.ProductRepository;
import com.javandroid.accounting_app.data.repository.CustomerRepository;
import com.javandroid.accounting_app.data.repository.UserRepository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Worker class that handles the daily database backup
 * It exports all data from the database to CSV files in the backups folder
 */
public class DatabaseBackupWorker extends Worker {
    private static final String TAG = "DatabaseBackupWorker";
    private static final String BACKUP_FOLDER_NAME = "accounting_app_backups";

    private AppDatabase database;
    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private CustomerRepository customerRepository;
    private UserRepository userRepository;

    public DatabaseBackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        database = AppDatabase.getInstance(context);
        orderRepository = new OrderRepository(context);
        productRepository = new ProductRepository(context);
        customerRepository = new CustomerRepository(context);
        userRepository = new UserRepository(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "Starting daily database backup");

            // Create the backup directory
            File backupDir = createBackupDirectory();
            if (backupDir == null) {
                Log.e(TAG, "Failed to create backup directory");
                return Result.failure();
            }

            // Create timestamped folder for today's backup
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault()).format(new Date());
            File todayBackupDir = new File(backupDir, timestamp);
            if (!todayBackupDir.exists() && !todayBackupDir.mkdirs()) {
                Log.e(TAG, "Failed to create today's backup directory");
                return Result.failure();
            }

            // Export each data type
            boolean success = true;
            success &= exportOrders(todayBackupDir);
            success &= exportProducts(todayBackupDir);
            success &= exportCustomers(todayBackupDir);
            success &= exportUsers(todayBackupDir);

            Log.d(TAG, "Database backup " + (success ? "completed successfully" : "completed with errors"));

            // Clean up old backups (keep only last 7 days)
            cleanupOldBackups(backupDir, 7);

            return success ? Result.success() : Result.failure();
        } catch (Exception e) {
            Log.e(TAG, "Error during backup", e);
            return Result.failure();
        }
    }

    private File createBackupDirectory() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File backupDir = new File(downloadsDir, BACKUP_FOLDER_NAME);

        if (!backupDir.exists() && !backupDir.mkdirs()) {
            Log.e(TAG, "Failed to create backup directory at " + backupDir.getAbsolutePath());
            return null;
        }

        return backupDir;
    }

    private boolean exportOrders(File backupDir) {
        try {
            // Export orders
            File ordersFile = new File(backupDir, "orders.csv");
            File orderItemsFile = new File(backupDir, "order_items.csv");

            // Get all orders
            CountDownLatch latch = new CountDownLatch(1);
            final List<OrderEntity>[] orders = new List[1];

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getApplicationContext().getMainExecutor().execute(() -> {
                    orderRepository.getAllOrders().observeForever(orderList -> {
                        orders[0] = orderList;
                        latch.countDown();
                    });
                });
            }

            if (!latch.await(30, TimeUnit.SECONDS)) {
                Log.e(TAG, "Timeout waiting for orders data");
                return false;
            }

            if (orders[0] == null || orders[0].isEmpty()) {
                Log.d(TAG, "No orders to export");
                return true;
            }

            // Export orders to CSV
            try (FileWriter writer = new FileWriter(ordersFile)) {
                writer.append("Order ID,Date,Customer ID,User ID,Total\n");

                for (OrderEntity order : orders[0]) {
                    writer.append(String.valueOf(order.getOrderId())).append(",");
                    writer.append(escapeCsvField(order.getDate())).append(",");
                    writer.append(String.valueOf(order.getCustomerId())).append(",");
                    writer.append(String.valueOf(order.getUserId())).append(",");
                    writer.append(String.valueOf(order.getTotal())).append("\n");
                }
                writer.flush();
            }

            // For each order, get its items
            try (FileWriter writer = new FileWriter(orderItemsFile)) {
                writer.append("Item ID,Order ID,Product ID,Product Name,Barcode,Quantity,Buy Price,Sell Price\n");

                for (OrderEntity order : orders[0]) {
                    final List<OrderItemEntity>[] items = new List[1];
                    CountDownLatch itemLatch = new CountDownLatch(1);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        getApplicationContext().getMainExecutor().execute(() -> {
                            orderRepository.getOrderItems(order.getOrderId()).observeForever(itemList -> {
                                items[0] = itemList;
                                itemLatch.countDown();
                            });
                        });
                    }

                    if (!itemLatch.await(5, TimeUnit.SECONDS)) {
                        Log.e(TAG, "Timeout waiting for order items for order " + order.getOrderId());
                        continue;
                    }

                    if (items[0] == null || items[0].isEmpty()) {
                        continue;
                    }

                    for (OrderItemEntity item : items[0]) {
                        writer.append(String.valueOf(item.getItemId())).append(",");
                        writer.append(String.valueOf(item.getOrderId())).append(",");
                        writer.append(item.getProductId() != null ? String.valueOf(item.getProductId()) : "")
                                .append(",");
                        writer.append(escapeCsvField(item.getProductName())).append(",");
                        writer.append(escapeCsvField(item.getBarcode())).append(",");
                        writer.append(String.valueOf(item.getQuantity())).append(",");
                        writer.append(String.valueOf(item.getBuyPrice())).append(",");
                        writer.append(String.valueOf(item.getSellPrice())).append("\n");
                    }
                }
                writer.flush();
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error exporting orders", e);
            return false;
        }
    }

    private boolean exportProducts(File backupDir) {
        try {
            File productsFile = new File(backupDir, "products.csv");

            // Get all products
            CountDownLatch latch = new CountDownLatch(1);
            final List<ProductEntity>[] products = new List[1];

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getApplicationContext().getMainExecutor().execute(() -> {
                    productRepository.getAllProducts().observeForever(productList -> {
                        products[0] = productList;
                        latch.countDown();
                    });
                });
            }

            if (!latch.await(30, TimeUnit.SECONDS)) {
                Log.e(TAG, "Timeout waiting for products data");
                return false;
            }

            if (products[0] == null || products[0].isEmpty()) {
                Log.d(TAG, "No products to export");
                return true;
            }

            // Export products to CSV
            try (FileWriter writer = new FileWriter(productsFile)) {
                writer.append("Product ID,Name,Barcode,Buy Price,Sell Price,Stock\n");

                for (ProductEntity product : products[0]) {
                    writer.append(String.valueOf(product.getProductId())).append(",");
                    writer.append(escapeCsvField(product.getName())).append(",");
                    writer.append(escapeCsvField(product.getBarcode())).append(",");
                    writer.append(String.valueOf(product.getBuyPrice())).append(",");
                    writer.append(String.valueOf(product.getSellPrice())).append(",");
                    writer.append(String.valueOf(product.getStock())).append("\n");
                }
                writer.flush();
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error exporting products", e);
            return false;
        }
    }

    private boolean exportCustomers(File backupDir) {
        try {
            File customersFile = new File(backupDir, "customers.csv");

            // Get all customers
            CountDownLatch latch = new CountDownLatch(1);
            final List<CustomerEntity>[] customers = new List[1];

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getApplicationContext().getMainExecutor().execute(() -> {
                    customerRepository.getAllCustomers().observeForever(customerList -> {
                        customers[0] = customerList;
                        latch.countDown();
                    });
                });
            }

            if (!latch.await(30, TimeUnit.SECONDS)) {
                Log.e(TAG, "Timeout waiting for customers data");
                return false;
            }

            if (customers[0] == null || customers[0].isEmpty()) {
                Log.d(TAG, "No customers to export");
                return true;
            }

            // Export customers to CSV
            try (FileWriter writer = new FileWriter(customersFile)) {
                writer.append("Customer ID,Name\n");

                for (CustomerEntity customer : customers[0]) {
                    writer.append(String.valueOf(customer.getCustomerId())).append(",");
                    writer.append(escapeCsvField(customer.getName())).append("\n");
                }
                writer.flush();
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error exporting customers", e);
            return false;
        }
    }

    private boolean exportUsers(File backupDir) {
        try {
            File usersFile = new File(backupDir, "users.csv");

            // Get all users
            CountDownLatch latch = new CountDownLatch(1);
            final List<UserEntity>[] users = new List[1];

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getApplicationContext().getMainExecutor().execute(() -> {
                    userRepository.getAllUsers().observeForever(userList -> {
                        users[0] = userList;
                        latch.countDown();
                    });
                });
            }

            if (!latch.await(30, TimeUnit.SECONDS)) {
                Log.e(TAG, "Timeout waiting for users data");
                return false;
            }

            if (users[0] == null || users[0].isEmpty()) {
                Log.d(TAG, "No users to export");
                return true;
            }

            // Export users to CSV
            try (FileWriter writer = new FileWriter(usersFile)) {
                writer.append("User ID,Username,Password\n");

                for (UserEntity user : users[0]) {
                    writer.append(String.valueOf(user.getUserId())).append(",");
                    writer.append(escapeCsvField(user.getUsername())).append(",");
                    writer.append(escapeCsvField(user.getPassword())).append("\n");
                }
                writer.flush();
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error exporting users", e);
            return false;
        }
    }

    private void cleanupOldBackups(File backupDir, int keepDays) {
        File[] backupFolders = backupDir.listFiles();
        if (backupFolders == null || backupFolders.length <= keepDays) {
            return;
        }

        // Get current time minus keepDays
        long cutoffTime = System.currentTimeMillis() - (keepDays * 24 * 60 * 60 * 1000L);

        for (File folder : backupFolders) {
            if (folder.isDirectory() && folder.lastModified() < cutoffTime) {
                deleteRecursive(folder);
            }
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }

        // If the field contains commas, quotes, or newlines, wrap it in quotes
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            // Replace any quotes with double quotes (CSV standard for escaping quotes)
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}