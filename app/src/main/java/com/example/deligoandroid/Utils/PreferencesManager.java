package com.example.deligoandroid.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREF_NAME = "DeliGoPrefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_SELECTED_TAB = "selected_tab";

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

    public void setLanguage(String languageCode) {
        preferences.edit().putString(KEY_LANGUAGE, languageCode).apply();
    }

    public String getLanguage() {
        return preferences.getString(KEY_LANGUAGE, "en");
    }

    public void setSelectedTabId(int itemId) {
        preferences.edit().putInt(KEY_SELECTED_TAB, itemId).apply();
    }

    public int getSelectedTabId(int defaultId) {
        return preferences.getInt(KEY_SELECTED_TAB, defaultId);
    }
} 