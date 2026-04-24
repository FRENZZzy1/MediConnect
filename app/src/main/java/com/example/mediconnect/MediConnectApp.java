package com.example.mediconnect;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Custom Application class.
 * Applies the saved dark-mode preference once, before any Activity is created.
 * This ensures the correct theme is used from the very first frame on cold starts.
 */
public class MediConnectApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        applySavedDarkMode();
    }

    private void applySavedDarkMode() {
        SharedPreferences prefs = getSharedPreferences(
                ProfileFragment.PREFS_NAME, MODE_PRIVATE);
        int savedMode = prefs.getInt(
                ProfileFragment.KEY_DARK_MODE,
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedMode);
    }
}