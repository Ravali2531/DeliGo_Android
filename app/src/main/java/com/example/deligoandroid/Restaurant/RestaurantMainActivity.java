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

        // Restore the selected tab and set initial fragment
        int savedItemId = preferencesManager.getSelectedTabId(R.id.navigation_orders);
        
        // Create and set the initial fragment
        Fragment initialFragment;
        if (savedItemId == R.id.navigation_orders) {
            initialFragment = new OrdersFragment();
        } else if (savedItemId == R.id.navigation_menu) {
            initialFragment = new MenuFragment();
        } else {
            initialFragment = new AccountFragment();
        }

        // Set the selected tab
        bottomNavigationView.setSelectedItemId(savedItemId);

        // Set initial fragment
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.nav_host_fragment_activity_restaurant_main, initialFragment)
            .commit();
    }
} 