package com.example.deligoandroid;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.deligoandroid.Utils.PreferencesManager;

public class DeliGoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initializeTheme();
    }

    private void initializeTheme() {
        PreferencesManager preferencesManager = new PreferencesManager(this);
        int nightMode = preferencesManager.isDarkMode() 
            ? AppCompatDelegate.MODE_NIGHT_YES 
            : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
} 