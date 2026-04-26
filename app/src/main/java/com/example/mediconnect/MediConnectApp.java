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
        applyDoctorDarkMode();
    }

    private void applySavedDarkMode() {
        SharedPreferences prefs = getSharedPreferences(
                ProfileFragment.PREFS_NAME, MODE_PRIVATE);
        int savedMode = prefs.getInt(
                ProfileFragment.KEY_DARK_MODE,
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedMode);
    }

    private void applyDoctorDarkMode() {
        SharedPreferences prefs = getSharedPreferences(
                DoctorPersonalProfile.DOCTOR_PREFS_NAME, MODE_PRIVATE);
        int savedMode = prefs.getInt(
                DoctorPersonalProfile.DOCTOR_KEY_DARK_MODE,
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Only override the system default when an explicit choice was made
        if (savedMode != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            AppCompatDelegate.setDefaultNightMode(savedMode);
        }
    }



}