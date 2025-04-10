package com.example.deligoandroid.Driver;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Authentication.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.content.Intent;

public class DocumentsUnderReviewActivity extends AppCompatActivity {
    private DatabaseReference databaseRef;
    private ValueEventListener statusListener;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents_under_review);

        mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference()
                .child("drivers")
                .child(userId);

        // Setup logout button
        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> logout());

        // Listen for status changes
        statusListener = databaseRef.child("documents/status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.getValue(String.class);
                    if (status != null && status.equals("approved")) {
                        // If documents are approved, navigate to DriverHomeActivity
                        startActivity(new Intent(DocumentsUnderReviewActivity.this, DriverHomeActivity.class));
                        finish();
                    } else if (status != null && status.equals("rejected")) {
                        // If documents are rejected, show rejection message and allow resubmission
                        TextView statusText = findViewById(R.id.statusText);
                        statusText.setText("Your documents were not approved. Please check your email for details and resubmit.");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error
            }
        });
    }

    private void logout() {
        // Sign out from Firebase
        mAuth.signOut();
        
        // Navigate to login screen
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusListener != null) {
            databaseRef.child("documents/status").removeEventListener(statusListener);
        }
    }
} 