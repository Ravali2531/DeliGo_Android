package com.example.deligoandroid.Driver;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

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
    private DatabaseReference databaseRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_home);

        // Initialize views
        pendingReviewLayout = findViewById(R.id.pendingReviewLayout);
        normalHomeLayout = findViewById(R.id.normalHomeLayout);

        // Initialize Firebase
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // Check document status
        checkDocumentStatus();
    }

    private void checkDocumentStatus() {
        databaseRef.child("drivers")
                .child(userId)
                .child("documents")
                .child("status")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String status = dataSnapshot.getValue(String.class);
                        updateUI(status);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Handle error
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
        }
    }
} 