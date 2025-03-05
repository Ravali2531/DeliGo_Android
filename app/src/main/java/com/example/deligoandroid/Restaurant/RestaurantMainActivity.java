package com.example.deligoandroid.Restaurant;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Utils.PreferencesManager;
import com.example.deligoandroid.Restaurant.Fragments.OrdersFragment;
import com.example.deligoandroid.Restaurant.Fragments.MenuFragment;
import com.example.deligoandroid.Restaurant.Fragments.AccountFragment;

public class RestaurantMainActivity extends AppCompatActivity {
    private PreferencesManager preferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_main);

        // Initialize PreferencesManager
        preferencesManager = new PreferencesManager(this);

        // Setup Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        
        // Restore the selected tab
        int savedItemId = preferencesManager.getSelectedTabId(R.id.navigation_orders);
        bottomNavigationView.setSelectedItemId(savedItemId);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_orders) {
                selectedFragment = new OrdersFragment();
            } else if (itemId == R.id.navigation_menu) {
                selectedFragment = new MenuFragment();
            } else if (itemId == R.id.navigation_account) {
                selectedFragment = new AccountFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment_activity_restaurant_main, selectedFragment)
                    .commit();
                
                // Save the selected tab position
                preferencesManager.setSelectedTabId(itemId);
                return true;
            }
            return false;
        });

        // Set initial fragment if this is the first creation
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment_activity_restaurant_main, new OrdersFragment())
                .commit();
        }
    }
} 