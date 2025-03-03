package com.example.deligoandroid.Admin;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.Toast;

import com.example.deligoandroid.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class  ManageUsersActivity extends AppCompatActivity {
    private Button customersTab, driversTab, restaurantsTab;
    private RecyclerView usersRecyclerView;
    private DatabaseReference databaseRef;
    private UserDocumentsAdapter adapter;
    private String currentUserType = "customers";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        // Initialize Firebase
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        customersTab = findViewById(R.id.customersTab);
        driversTab = findViewById(R.id.driversTab);
        restaurantsTab = findViewById(R.id.restaurantsTab);
        usersRecyclerView = findViewById(R.id.usersRecyclerView);

        // Setup RecyclerView
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserDocumentsAdapter();
        usersRecyclerView.setAdapter(adapter);

        // Setup click listeners
        customersTab.setOnClickListener(v -> showUserType("customers"));
        driversTab.setOnClickListener(v -> showUserType("drivers"));
        restaurantsTab.setOnClickListener(v -> showUserType("restaurants"));

        // Show customers by default
        showUserType("customers");
    }

    private void showUserType(String userType) {
        currentUserType = userType;
        resetTabColors();
        
        switch (userType) {
            case "customers":
                customersTab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.orange)));
                break;
            case "drivers":
                driversTab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.orange)));
                break;
            case "restaurants":
                restaurantsTab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.orange)));
                break;
        }
        
        loadUsers(userType);
    }

    private void resetTabColors() {
        int gray = Color.parseColor("#CCCCCC");
        customersTab.setBackgroundTintList(ColorStateList.valueOf(gray));
        driversTab.setBackgroundTintList(ColorStateList.valueOf(gray));
        restaurantsTab.setBackgroundTintList(ColorStateList.valueOf(gray));
    }

    private void loadUsers(String userType) {
        databaseRef.child(userType).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<UserDocument> users = new ArrayList<>();
                
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    UserDocument user = new UserDocument();
                    user.userId = userSnapshot.getKey();
                    user.userType = userType;
                    user.name = userSnapshot.child("fullName").getValue(String.class);
                    user.email = userSnapshot.child("email").getValue(String.class);
                    user.phone = userSnapshot.child("phone").getValue(String.class);
                    user.address = userSnapshot.child("address").getValue(String.class);

                    if (userType.equals("restaurants")) {
                        user.restaurantName = userSnapshot.child("restaurantName").getValue(String.class);
                        user.openingHours = userSnapshot.child("hours/opening").getValue(String.class);
                        user.closingHours = userSnapshot.child("hours/closing").getValue(String.class);
                    }

                    if (userType.equals("drivers") || userType.equals("restaurants")) {
                        user.documentsSubmitted = userSnapshot.child("documentsSubmitted").getValue(Boolean.class);
                        if (user.documentsSubmitted != null && user.documentsSubmitted) {
                            user.documentStatus = userSnapshot.child("documents/status").getValue(String.class);
                            
                            if (userType.equals("drivers")) {
                                user.document1Url = userSnapshot.child("documents/files/govt_id/url").getValue(String.class);
                                user.document2Url = userSnapshot.child("documents/files/license/url").getValue(String.class);
                            } else {
                                user.document1Url = userSnapshot.child("documents/files/restaurant_proof/url").getValue(String.class);
                                user.document2Url = userSnapshot.child("documents/files/owner_id/url").getValue(String.class);
                            }
                        }
                    }
                    
                    users.add(user);
                }
                
                adapter.setDocuments(users, userType);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ManageUsersActivity.this, 
                    "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    void updateDocumentStatus(UserDocument document, String status) {
        DatabaseReference userRef = databaseRef.child(document.userType).child(document.userId);
        userRef.child("documents/status").setValue(status)
            .addOnSuccessListener(aVoid -> {
                String message = status.equals("approved") ? "Document approved!" : "Document rejected!";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
} 