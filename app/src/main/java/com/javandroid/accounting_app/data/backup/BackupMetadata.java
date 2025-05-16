package com.javandroid.accounting_app.data.backup;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utility class to manage backup metadata and statistics
 * Tracks successful/failed backups, size, and provides information about backup
 * status
 */
public class BackupMetadata {
    private static final String TAG = "BackupMetadata";
    private static final String PREFS_NAME = "BackupMetadataPrefs";
    private static final String KEY_LAST_BACKUP_DATE = "last_backup_date";
    private static final String KEY_LAST_BACKUP_SIZE = "last_backup_size";
    private static final String KEY_LAST_BACKUP_SUCCESS = "last_backup_success";
    private static final String KEY_TOTAL_BACKUPS = "total_backups";
    private static final String KEY_SUCCESSFUL_BACKUPS = "successful_backups";
    private static final String KEY_FAILED_BACKUPS = "failed_backups";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String BACKUP_FOLDER_NAME = "accounting_app_backups";

    /**
     * Record a successful backup operation
     *
     * @param context    Application context
     * @param backupPath Path to the backup directory
     */
    public static void recordSuccessfulBackup(Context context, String backupPath) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Update counters
        int totalBackups = prefs.getInt(KEY_TOTAL_BACKUPS, 0) + 1;
        int successfulBackups = prefs.getInt(KEY_SUCCESSFUL_BACKUPS, 0) + 1;

        editor.putInt(KEY_TOTAL_BACKUPS, totalBackups);
        editor.putInt(KEY_SUCCESSFUL_BACKUPS, successfulBackups);
        editor.putBoolean(KEY_LAST_BACKUP_SUCCESS, true);

        // Record current date/time
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        String currentDate = sdf.format(new Date());
        editor.putString(KEY_LAST_BACKUP_DATE, currentDate);

        // Calculate and record backup size
        long size = calculateBackupSize(backupPath);
        editor.putLong(KEY_LAST_BACKUP_SIZE, size);

        editor.apply();
        Log.d(TAG, "Recorded successful backup: " + currentDate + ", size: " + formatSize(size));
    }

    /**
     * Record a failed backup operation
     *
     * @param context Application context
     */
    public static void recordFailedBackup(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Update counters
        int totalBackups = prefs.getInt(KEY_TOTAL_BACKUPS, 0) + 1;
        int failedBackups = prefs.getInt(KEY_FAILED_BACKUPS, 0) + 1;

        editor.putInt(KEY_TOTAL_BACKUPS, totalBackups);
        editor.putInt(KEY_FAILED_BACKUPS, failedBackups);
        editor.putBoolean(KEY_LAST_BACKUP_SUCCESS, false);

        // Record current date/time
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        String currentDate = sdf.format(new Date());
        editor.putString(KEY_LAST_BACKUP_DATE, currentDate);

        editor.apply();
        Log.d(TAG, "Recorded failed backup: " + currentDate);
    }

    /**
     * Get the date and time of the last backup operation
     *
     * @param context Application context
     * @return Date of last backup or null if no backup has been performed
     */
    public static Date getLastBackupDate(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String dateString = prefs.getString(KEY_LAST_BACKUP_DATE, null);

        if (dateString == null) {
            return null;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
            return sdf.parse(dateString);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing backup date", e);
            return null;
        }
    }

    /**
     * Get the formatted date string of the last backup
     *
     * @param context Application context
     * @return Formatted date string or "Never" if no backup has been performed
     */
    public static String getLastBackupDateString(Context context) {
        Date lastBackup = getLastBackupDate(context);
        if (lastBackup == null) {
            return "Never";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        return sdf.format(lastBackup);
    }

    /**
     * Get the size of the last backup
     *
     * @param context Application context
     * @return Size of the last backup in bytes or 0 if no backup exists
     */
    public static long getLastBackupSize(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_LAST_BACKUP_SIZE, 0);
    }

    /**
     * Get the formatted size of the last backup
     *
     * @param context Application context
     * @return Formatted size string (e.g., "1.2 MB")
     */
    public static String getLastBackupSizeFormatted(Context context) {
        long size = getLastBackupSize(context);
        return formatSize(size);
    }

    /**
     * Check if the last backup was successful
     *
     * @param context Application context
     * @return true if the last backup was successful, false otherwise
     */
    public static boolean wasLastBackupSuccessful(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_LAST_BACKUP_SUCCESS, false);
    }

    /**
     * Get backup statistics
     *
     * @param context Application context
     * @return Array of [total, successful, failed] backup counts
     */
    public static int[] getBackupStats(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int total = prefs.getInt(KEY_TOTAL_BACKUPS, 0);
        int successful = prefs.getInt(KEY_SUCCESSFUL_BACKUPS, 0);
        int failed = prefs.getInt(KEY_FAILED_BACKUPS, 0);

        return new int[] { total, successful, failed };
    }

    /**
     * List all available backup directories
     *
     * @param context Application context
     * @return List of backup directory file objects
     */
    public static List<File> listAllBackups(Context context) {
        List<File> backupDirs = new ArrayList<>();
        File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS);
        File backupDir = new File(downloadsDir, BACKUP_FOLDER_NAME);

        if (!backupDir.exists() || !backupDir.isDirectory()) {
            return backupDirs;
        }

        File[] files = backupDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    backupDirs.add(file);
                }
            }
        }

        return backupDirs;
    }

    /**
     * Calculate the size of a backup directory
     *
     * @param path Path to the backup directory
     * @return Size in bytes
     */
    private static long calculateBackupSize(String path) {
        if (path == null) {
            return 0;
        }

        File directory = new File(path);
        return calculateDirSize(directory);
    }

    /**
     * Calculate the size of a directory and its contents
     *
     * @param dir Directory to calculate size for
     * @return Size in bytes
     */
    private static long calculateDirSize(File dir) {
        if (dir == null || !dir.exists()) {
            return 0;
        }

        long size = 0;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else {
                        size += calculateDirSize(file);
                    }
                }
            }
        } else {
            size = dir.length();
        }

        return size;
    }

    /**
     * Format a size in bytes to a human-readable string
     *
     * @param size Size in bytes
     * @return Formatted string (e.g., "1.2 MB")
     */
    private static String formatSize(long size) {
        if (size <= 0) {
            return "0 B";
        }

        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.getDefault(), "%.1f %s",
                size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}