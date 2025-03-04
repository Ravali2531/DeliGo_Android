package com.example.deligoandroid.Restaurant;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.Fragment;

import com.example.deligoandroid.Authentication.LoginActivity;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.Adapters.MenuAdapter;
import com.example.deligoandroid.Restaurant.Adapters.OrdersAdapter;
import com.example.deligoandroid.Restaurant.Models.MenuItemModel;
import com.example.deligoandroid.Restaurant.Models.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import android.content.res.ColorStateList;

import com.example.deligoandroid.Restaurant.Fragments.OrdersFragment;
import com.example.deligoandroid.Restaurant.Fragments.MenuFragment;
import com.example.deligoandroid.Restaurant.Fragments.AccountFragment;

import android.widget.ImageView;

public class RestaurantHomeActivity extends AppCompatActivity {
    private LinearLayout pendingReviewLayout;
    private LinearLayout normalHomeLayout;
    private TextView restaurantNameText;
    private Switch restaurantStatusSwitch;
    private Button logoutButton;
    private DatabaseReference databaseRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_home);

        // Initialize Firebase and user ID first
        databaseRef = FirebaseDatabase.getInstance().getReference();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize views
        initializeViews();
        
        // Load data
        loadInitialData();
        
        // Setup remaining components
        setupClickListeners();
        checkDocumentStatus();

        // Show Orders fragment by default
        if (savedInstanceState == null) {
            loadFragment(new OrdersFragment());
        }
    }

    private void initializeViews() {
        pendingReviewLayout = findViewById(R.id.pendingReviewLayout);
        normalHomeLayout = findViewById(R.id.normalHomeLayout);
        restaurantNameText = findViewById(R.id.restaurantNameText);
        restaurantStatusSwitch = findViewById(R.id.restaurantStatusSwitch);
        logoutButton = findViewById(R.id.logoutButton);
    }

    private void loadInitialData() {
        // Load restaurant name
        databaseRef.child("restaurants").child(userId).child("restaurantName")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String name = dataSnapshot.getValue(String.class);
                    if (name != null) {
                        restaurantNameText.setText(name);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(RestaurantHomeActivity.this, 
                        "Error loading restaurant name", Toast.LENGTH_SHORT).show();
                }
            });

        // Load initial restaurant status
        databaseRef.child("restaurants").child(userId).child("isOpen")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Boolean isOpen = dataSnapshot.getValue(Boolean.class);
                    if (isOpen != null) {
                        restaurantStatusSwitch.setChecked(isOpen);
                        updateSwitchAppearance(isOpen);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(RestaurantHomeActivity.this, 
                        "Error loading restaurant status", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void setupClickListeners() {
        // Bottom navigation
        findViewById(R.id.ordersTab).setOnClickListener(v -> {
            loadFragment(new OrdersFragment());
            updateTabColors(R.id.ordersTab);
        });
        findViewById(R.id.menuTab).setOnClickListener(v -> {
            loadFragment(new MenuFragment());
            updateTabColors(R.id.menuTab);
        });
        findViewById(R.id.accountTab).setOnClickListener(v -> {
            loadFragment(new AccountFragment());
            updateTabColors(R.id.accountTab);
        });
        
        restaurantStatusSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateSwitchAppearance(isChecked);
            updateRestaurantStatus(isChecked);
        });
            
        logoutButton.setOnClickListener(v -> handleLogout());

        // Set initial tab colors
        updateTabColors(R.id.ordersTab);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit();
    }

    private void updateRestaurantStatus(boolean isOpen) {
        databaseRef.child("restaurants").child(userId).child("isOpen")
            .setValue(isOpen)
            .addOnSuccessListener(aVoid -> {
                String status = isOpen ? "open" : "closed";
                Toast.makeText(this, "Restaurant is now " + status, Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                restaurantStatusSwitch.setChecked(!isOpen); // Revert switch
                Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
            });
    }

    private void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void checkDocumentStatus() {
        databaseRef.child("restaurants")
                .child(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Check if documents are submitted
                            Boolean documentsSubmitted = dataSnapshot.child("documentsSubmitted").getValue(Boolean.class);
                            if (documentsSubmitted != null && documentsSubmitted) {
                                // Documents are submitted, check status
                                DataSnapshot statusSnapshot = dataSnapshot.child("documents").child("status");
                                String status = statusSnapshot.getValue(String.class);
                                updateUI(status);
                            } else {
                                // Documents not submitted
                                updateUI("not_submitted");
                            }
                        } else {
                            // Restaurant data doesn't exist
                            Toast.makeText(RestaurantHomeActivity.this, 
                                "Error: Restaurant data not found", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(RestaurantHomeActivity.this, 
                            "Error: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateUI(String status) {
        if (status == null || status.equals("pending_review")) {
            pendingReviewLayout.setVisibility(View.VISIBLE);
            normalHomeLayout.setVisibility(View.GONE);
        } else if (status.equals("approved")) {
            pendingReviewLayout.setVisibility(View.GONE);
            normalHomeLayout.setVisibility(View.VISIBLE);
        } else if (status.equals("not_submitted")) {
            // Redirect to document upload
            finish();
        }
    }

    private void updateSwitchAppearance(boolean isOpen) {
        if (isOpen) {
            restaurantStatusSwitch.setText("Open");
            restaurantStatusSwitch.setTextColor(Color.parseColor("#4CAF50")); // Material Green
        } else {
            restaurantStatusSwitch.setText("Closed");
            restaurantStatusSwitch.setTextColor(Color.parseColor("#F44336")); // Material Red
        }
    }

    private void updateTabColors(int selectedTabId) {
        // Get all tab views
        View ordersTab = findViewById(R.id.ordersTab);
        View menuTab = findViewById(R.id.menuTab);
        View accountTab = findViewById(R.id.accountTab);

        // Reset all tabs to black
        updateTabColor(ordersTab, false);
        updateTabColor(menuTab, false);
        updateTabColor(accountTab, false);

        // Set selected tab to blue
        View selectedTab = findViewById(selectedTabId);
        updateTabColor(selectedTab, true);
    }

    private void updateTabColor(View tabView, boolean isSelected) {
        ImageView icon = (ImageView) ((LinearLayout) tabView).getChildAt(0);
        TextView text = (TextView) ((LinearLayout) tabView).getChildAt(1);

        int color = isSelected ? Color.parseColor("#2196F3") : Color.parseColor("#000000");
        icon.setImageTintList(ColorStateList.valueOf(color));
        text.setTextColor(color);
    }
} 