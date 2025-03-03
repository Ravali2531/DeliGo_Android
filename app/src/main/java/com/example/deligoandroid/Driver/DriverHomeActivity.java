package com.example.deligoandroid.Driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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

public class DriverHomeActivity extends AppCompatActivity {

    private LinearLayout pendingReviewLayout;
    private LinearLayout normalHomeLayout;
    private Button logoutButton;
    private DatabaseReference databaseRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_home);

        // Initialize views
        pendingReviewLayout = findViewById(R.id.pendingReviewLayout);
        normalHomeLayout = findViewById(R.id.normalHomeLayout);
        logoutButton = findViewById(R.id.logoutButton);

        // Set initial visibility
        pendingReviewLayout.setVisibility(View.GONE);
        normalHomeLayout.setVisibility(View.GONE);

        // Setup logout button
        logoutButton.setOnClickListener(v -> handleLogout());

        // Initialize Firebase
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // Check document status
        checkDocumentStatus();
    }

    private void checkDocumentStatus() {
        databaseRef.child("drivers")
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
                            // Driver data doesn't exist
                            Toast.makeText(DriverHomeActivity.this, 
                                "Error: Driver data not found", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(DriverHomeActivity.this, 
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

    private void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
} 