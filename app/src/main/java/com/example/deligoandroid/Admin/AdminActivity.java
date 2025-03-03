package com.example.deligoandroid.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deligoandroid.Authentication.LoginActivity;
import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminActivity extends AppCompatActivity {
    private TextView driverCountBadge, restaurantCountBadge;
    private Button userManagementBtn, chatManagementBtn, logoutButton;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Initialize Firebase
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        userManagementBtn = findViewById(R.id.userManagementBtn);
        chatManagementBtn = findViewById(R.id.chatManagementBtn);
        logoutButton = findViewById(R.id.logoutButton);
        driverCountBadge = findViewById(R.id.driverCountBadge);
        restaurantCountBadge = findViewById(R.id.restaurantCountBadge);

        // Setup click listeners
        userManagementBtn.setOnClickListener(v -> 
            startActivity(new Intent(this, ManageUsersActivity.class)));
        
        logoutButton.setOnClickListener(v -> handleLogout());
        
        // Setup document count listeners
        setupDocumentCountListeners();
    }

    private void setupDocumentCountListeners() {
        // Listen for pending driver documents
        databaseRef.child("drivers").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int pendingCount = 0;
                for (DataSnapshot driverSnapshot : dataSnapshot.getChildren()) {
                    Boolean documentsSubmitted = driverSnapshot.child("documentsSubmitted").getValue(Boolean.class);
                    String status = driverSnapshot.child("documents").child("status").getValue(String.class);
                    if (documentsSubmitted != null && documentsSubmitted && 
                        (status == null || status.equals("pending_review"))) {
                        pendingCount++;
                    }
                }
                updateDriverBadge(pendingCount);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(AdminActivity.this, 
                    "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Listen for pending restaurant documents
        databaseRef.child("restaurants").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int pendingCount = 0;
                for (DataSnapshot restaurantSnapshot : dataSnapshot.getChildren()) {
                    Boolean documentsSubmitted = restaurantSnapshot.child("documentsSubmitted").getValue(Boolean.class);
                    String status = restaurantSnapshot.child("documents").child("status").getValue(String.class);
                    if (documentsSubmitted != null && documentsSubmitted && 
                        (status == null || status.equals("pending_review"))) {
                        pendingCount++;
                    }
                }
                updateRestaurantBadge(pendingCount);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(AdminActivity.this, 
                    "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDriverBadge(int count) {
        if (count > 0) {
            driverCountBadge.setVisibility(View.VISIBLE);
            driverCountBadge.setText(String.valueOf(count));
        } else {
            driverCountBadge.setVisibility(View.GONE);
        }
    }

    private void updateRestaurantBadge(int count) {
        if (count > 0) {
            restaurantCountBadge.setVisibility(View.VISIBLE);
            restaurantCountBadge.setText(String.valueOf(count));
        } else {
            restaurantCountBadge.setVisibility(View.GONE);
        }
    }

    private void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
} 