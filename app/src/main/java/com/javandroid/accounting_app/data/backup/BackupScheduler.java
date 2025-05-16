package com.javandroid.accounting_app.data.backup;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Utility class to schedule daily database backups
 */
public class BackupScheduler {
    private static final String TAG = "BackupScheduler";
    private static final String BACKUP_WORK_NAME = "database_daily_backup";

    /**
     * Schedule a daily backup of the database
     * The backup will run when:
     * - The device is charging
     * - The device has sufficient battery (not low)
     * - The device is idle
     * 
     * @param context Application context
     */
    public static void scheduleDailyBackup(Context context) {
        Log.d(TAG, "Scheduling daily database backup");

        // Define constraints for when the backup should run
        Constraints constraints = new Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiresBatteryNotLow(true)
                .setRequiresDeviceIdle(true)
                .build();

        // Create a periodic work request that runs once a day
        PeriodicWorkRequest backupRequest = new PeriodicWorkRequest.Builder(
                DatabaseBackupWorker.class,
                24, // Repeat interval
                TimeUnit.HOURS, // Time unit
                1, // Flex interval (can run 1 hour earlier than scheduled)
                TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        // Schedule the work request
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        BACKUP_WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
                        backupRequest);

        Log.d(TAG, "Daily backup scheduled successfully");
    }

    /**
     * Cancel scheduled daily backups
     * 
     * @param context Application context
     */
    public static void cancelScheduledBackups(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(BACKUP_WORK_NAME);
        Log.d(TAG, "Scheduled backups canceled");
    }

    /**
     * Run a backup immediately
     * 
     * @param context Application context
     */
    public static void runBackupNow(Context context) {
        Log.d(TAG, "Running backup immediately");

        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build();

        androidx.work.OneTimeWorkRequest backupRequest = new androidx.work.OneTimeWorkRequest.Builder(
                DatabaseBackupWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueue(backupRequest);
    }
}