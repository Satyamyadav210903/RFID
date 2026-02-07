package com.ecartes.rfid_demo.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class ThemeHelper {
    private static final String PREF_DARK_MODE = "dark_mode_enabled";
    private static final String PREFS_NAME = "UserPrefs";
    
    public static boolean isDarkModeEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_DARK_MODE, false);
    }
    
    public static void setDarkMode(Context context, boolean isDarkMode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_DARK_MODE, isDarkMode).apply();
    }
    
    public static String getCurrentTheme(Context context) {
        boolean isDarkMode = isDarkModeEnabled(context);
        return isDarkMode ? "Theme.RFID_Demo.Dark" : "Theme.RFID_Demo.Light";
    }
}