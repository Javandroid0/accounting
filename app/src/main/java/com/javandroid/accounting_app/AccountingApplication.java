package com.javandroid.accounting_app;

import android.app.Application;
import android.util.Log;
import android.content.SharedPreferences;

import androidx.work.Configuration;

import com.javandroid.accounting_app.data.backup.BackupScheduler;

/**
 * Application class for the Accounting App
 * Initializes background work and daily backups
 */
public class AccountingApplication extends Application implements Configuration.Provider {
        private static final String TAG = "AccountingApplication";
        private static final String PREFS_NAME = "AccountingAppPrefs";
        private static final String PREF_BACKUP_INTERVAL = "backup_interval_hours";
        private static final String PREF_BACKUP_REQUIRES_CHARGING = "backup_requires_charging";
        private static final String PREF_BACKUP_REQUIRES_BATTERY_OK = "backup_requires_battery_ok";
        private static final String PREF_BACKUP_REQUIRES_IDLE = "backup_requires_idle";

        // Default backup settings
        private static final long DEFAULT_BACKUP_INTERVAL = 24; // hours
        private static final boolean DEFAULT_REQUIRES_CHARGING = true;
        private static final boolean DEFAULT_REQUIRES_BATTERY_OK = true;
        private static final boolean DEFAULT_REQUIRES_IDLE = true;

        @Override
        public void onCreate() {
                super.onCreate();
                Log.d(TAG, "Application initialized");

                // Load backup settings from preferences
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                long backupInterval = prefs.getLong(PREF_BACKUP_INTERVAL, DEFAULT_BACKUP_INTERVAL);
                boolean requiresCharging = prefs.getBoolean(PREF_BACKUP_REQUIRES_CHARGING, DEFAULT_REQUIRES_CHARGING);
                boolean requiresBatteryOk = prefs.getBoolean(PREF_BACKUP_REQUIRES_BATTERY_OK,
                                DEFAULT_REQUIRES_BATTERY_OK);
                boolean requiresIdle = prefs.getBoolean(PREF_BACKUP_REQUIRES_IDLE, DEFAULT_REQUIRES_IDLE);

                // Schedule daily backup with saved settings
                Log.d(TAG, "Configuring backup: interval=" + backupInterval + "h, charging=" +
                                requiresCharging + ", battery=" + requiresBatteryOk + ", idle=" + requiresIdle);

                BackupScheduler.scheduleDailyBackup(
                                this,
                                backupInterval,
                                requiresCharging,
                                requiresBatteryOk,
                                requiresIdle);
        }

        /**
         * Update backup settings and reschedule
         */
        public void updateBackupSettings(long intervalHours, boolean requiresCharging,
                        boolean requiresBatteryOk, boolean requiresIdle) {
                // Save new settings
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putLong(PREF_BACKUP_INTERVAL, intervalHours);
                editor.putBoolean(PREF_BACKUP_REQUIRES_CHARGING, requiresCharging);
                editor.putBoolean(PREF_BACKUP_REQUIRES_BATTERY_OK, requiresBatteryOk);
                editor.putBoolean(PREF_BACKUP_REQUIRES_IDLE, requiresIdle);
                editor.apply();

                // Cancel existing backup schedule
                BackupScheduler.cancelScheduledBackups(this);

                // Create new schedule with updated settings
                BackupScheduler.scheduleDailyBackup(
                                this,
                                intervalHours,
                                requiresCharging,
                                requiresBatteryOk,
                                requiresIdle);

                Log.d(TAG, "Backup settings updated and rescheduled");
        }

        @Override
        public Configuration getWorkManagerConfiguration() {
                return new Configuration.Builder()
                                .setMinimumLoggingLevel(Log.INFO)
                                .build();
        }
}