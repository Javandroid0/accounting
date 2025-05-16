package com.javandroid.accounting_app;

import android.app.Application;
import android.util.Log;

import androidx.work.Configuration;

import com.javandroid.accounting_app.data.backup.BackupScheduler;

/**
 * Application class for the Accounting App
 * Initializes background work and daily backups
 */
public class AccountingApplication extends Application implements Configuration.Provider {
    private static final String TAG = "AccountingApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application initialized");

        // Schedule daily backups
        BackupScheduler.scheduleDailyBackup(this);
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .build();
    }
}