package com.example.deligoandroid.Driver;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.deligoandroid.Driver.Fragments.DriverHomeFragment;
import com.example.deligoandroid.Driver.Fragments.DriverOrdersFragment;
import com.example.deligoandroid.Driver.Fragments.DriverAccountFragment;
import com.example.deligoandroid.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverHomeActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private DatabaseReference databaseRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_home);

        // Initialize Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = currentUser.getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference()
                .child("drivers").child(userId);

        // Initialize views
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnItemSelectedListener(item -> {
                Fragment fragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.navigation_home) {
                    fragment = new DriverHomeFragment();
                } else if (itemId == R.id.navigation_orders) {
                    fragment = new DriverOrdersFragment();
                } else if (itemId == R.id.navigation_account) {
                    fragment = new DriverAccountFragment();
                }

                return loadFragment(fragment);
            });
        }

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new DriverHomeFragment());
        }
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            try {
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
                return true;
            } catch (Exception e) {
                Toast.makeText(this, "Error loading screen", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
} 