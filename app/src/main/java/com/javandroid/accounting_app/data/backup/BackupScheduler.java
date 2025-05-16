package com.javandroid.accounting_app.data.backup;

import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.ListenableWorker;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import java.util.concurrent.TimeUnit;

/**
 * Utility class to schedule database backups
 * Provides methods for scheduled and immediate backups with configurable
 * constraints
 */
public class BackupScheduler {
        private static final String TAG = "BackupScheduler";
        private static final String BACKUP_WORK_NAME = "database_daily_backup";
        private static final long DEFAULT_BACKUP_INTERVAL_HOURS = 24;

        // Callback interface for backup operations
        public interface BackupCallback {
                void onBackupComplete(boolean success);
        }

        /**
         * Schedule a daily backup of the database with default settings
         * 
         * @param context Application context
         */
        public static void scheduleDailyBackup(Context context) {
                scheduleDailyBackup(context, DEFAULT_BACKUP_INTERVAL_HOURS, true, true, true);
        }

        /**
         * Schedule a backup of the database with custom settings
         * 
         * @param context               Application context
         * @param intervalHours         Hours between backups
         * @param requiresCharging      Whether device should be charging
         * @param requiresBatteryNotLow Whether battery should not be low
         * @param requiresDeviceIdle    Whether device should be idle
         */
        public static void scheduleDailyBackup(Context context, long intervalHours,
                        boolean requiresCharging,
                        boolean requiresBatteryNotLow,
                        boolean requiresDeviceIdle) {
                Log.d(TAG, "Scheduling backup every " + intervalHours + " hours");

                // Define constraints for when the backup should run
                Constraints constraints = new Constraints.Builder()
                                .setRequiresCharging(requiresCharging)
                                .setRequiresBatteryNotLow(requiresBatteryNotLow)
                                .setRequiresDeviceIdle(requiresDeviceIdle)
                                .build();

                // Create a periodic work request
                PeriodicWorkRequest backupRequest = new PeriodicWorkRequest.Builder(
                                DatabaseBackupWorker.class,
                                intervalHours, // Repeat interval
                                TimeUnit.HOURS, // Time unit
                                Math.min(intervalHours - 1, 1), // Flex interval (run 1 hour before scheduled time or
                                                                // less)
                                TimeUnit.HOURS)
                                .setConstraints(constraints)
                                .build();

                // Schedule the work request
                WorkManager.getInstance(context)
                                .enqueueUniquePeriodicWork(
                                                BACKUP_WORK_NAME,
                                                ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
                                                backupRequest);

                Log.d(TAG, "Backup scheduled successfully");
        }

        /**
         * Cancel scheduled backups
         * 
         * @param context Application context
         */
        public static void cancelScheduledBackups(Context context) {
                WorkManager.getInstance(context).cancelUniqueWork(BACKUP_WORK_NAME);
                Log.d(TAG, "Scheduled backups canceled");
        }

        /**
         * Run a backup immediately with minimal constraints
         * 
         * @param context Application context
         */
        public static void runBackupNow(Context context) {
                runBackupNow(context, false, null);
        }

        /**
         * Run a backup immediately with optional callback
         * 
         * @param context               Application context
         * @param requiresBatteryNotLow Whether battery should not be low
         * @param callback              Optional callback to notify when backup
         *                              completes
         */
        public static void runBackupNow(Context context, boolean requiresBatteryNotLow, BackupCallback callback) {
                Log.d(TAG, "Running backup immediately");

                Constraints constraints = new Constraints.Builder()
                                .setRequiresBatteryNotLow(requiresBatteryNotLow)
                                .build();

                OneTimeWorkRequest backupRequest = new OneTimeWorkRequest.Builder(
                                DatabaseBackupWorker.class)
                                .setConstraints(constraints)
                                .build();

                WorkManager.getInstance(context).enqueue(backupRequest);

                // If callback provided, observe work status
                if (callback != null && context instanceof LifecycleOwner) {
                        WorkManager.getInstance(context).getWorkInfoByIdLiveData(backupRequest.getId())
                                        .observe((LifecycleOwner) context, workInfo -> {
                                                if (workInfo != null && workInfo.getState().isFinished()) {
                                                        boolean success = workInfo
                                                                        .getState() == WorkInfo.State.SUCCEEDED;
                                                        callback.onBackupComplete(success);
                                                }
                                        });
                }
        }

        /**
         * Check if a scheduled backup is currently active
         * 
         * @param context        Application context
         * @param lifecycleOwner Lifecycle owner for observation
         * @param observer       Observer to receive work info updates
         */
        public static void getScheduledBackupStatus(Context context, LifecycleOwner lifecycleOwner,
                        Observer<WorkInfo> observer) {
                WorkManager.getInstance(context)
                                .getWorkInfosForUniqueWorkLiveData(BACKUP_WORK_NAME)
                                .observe(lifecycleOwner, workInfos -> {
                                        if (workInfos != null && !workInfos.isEmpty()) {
                                                observer.onChanged(workInfos.get(0));
                                        }
                                });
        }
}