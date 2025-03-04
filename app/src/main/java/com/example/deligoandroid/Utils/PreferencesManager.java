package com.example.deligoandroid.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREF_NAME = "DeliGoPrefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    private final SharedPreferences preferences;

    public PreferencesManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setDarkMode(boolean isDarkMode) {
        preferences.edit().putBoolean(KEY_DARK_MODE, isDarkMode).apply();
    }

    public boolean isDarkMode() {
        return preferences.getBoolean(KEY_DARK_MODE, false);
    }
} 