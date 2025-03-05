package com.example.deligoandroid.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.deligoandroid.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RestaurantDetailsActivity extends AppCompatActivity {
    private TextView restaurantName, restaurantEmail, restaurantPhone;
    private TextView documentStatus, openingHours, closingHours;
    private View viewLicenseButton, viewIdButton, actionsLayout;
    private DatabaseReference databaseRef;
    private String restaurantId;
    private String licenseUrl, idUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_details);

        // Initialize views
        restaurantName = findViewById(R.id.restaurantName);
        restaurantEmail = findViewById(R.id.restaurantEmail);
        restaurantPhone = findViewById(R.id.restaurantPhone);
        documentStatus = findViewById(R.id.documentStatus);
        openingHours = findViewById(R.id.openingHours);
        closingHours = findViewById(R.id.closingHours);
        viewLicenseButton = findViewById(R.id.viewLicenseButton);
        viewIdButton = findViewById(R.id.viewIdButton);
        actionsLayout = findViewById(R.id.actionsLayout);

        // Get restaurant ID from intent
        restaurantId = getIntent().getStringExtra("restaurantId");
        if (restaurantId == null) {
            finish();
            return;
        }

        // Initialize Firebase
        databaseRef = FirebaseDatabase.getInstance().getReference()
            .child("restaurants").child(restaurantId);

        // Load restaurant data
        loadRestaurantData();

        // Setup click listeners
        findViewById(R.id.doneButton).setOnClickListener(v -> finish());
        
        viewLicenseButton.setOnClickListener(v -> {
            if (licenseUrl != null) {
                Intent intent = new Intent(this, DocumentPreviewActivity.class);
                intent.putExtra(DocumentPreviewActivity.EXTRA_IMAGE_URL, licenseUrl);
                startActivity(intent);
            }
        });

        viewIdButton.setOnClickListener(v -> {
            if (idUrl != null) {
                Intent intent = new Intent(this, DocumentPreviewActivity.class);
                intent.putExtra(DocumentPreviewActivity.EXTRA_IMAGE_URL, idUrl);
                startActivity(intent);
            }
        });

        findViewById(R.id.approveButton).setOnClickListener(v -> updateStatus("approved"));
        findViewById(R.id.rejectButton).setOnClickListener(v -> updateStatus("rejected"));
    }

    private void loadRestaurantData() {
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    finish();
                    return;
                }

                // Set basic info
                restaurantName.setText(dataSnapshot.child("restaurantName").getValue(String.class));
                restaurantEmail.setText(dataSnapshot.child("email").getValue(String.class));
                restaurantPhone.setText(dataSnapshot.child("phone").getValue(String.class));

                // Set document status
                String status = dataSnapshot.child("documents/status").getValue(String.class);
                documentStatus.setText(status != null ? status : "Pending");
                documentStatus.setTextColor(getResources().getColor(
                    status != null && status.equals("approved") ? R.color.green : R.color.orange));

                // Show/hide actions based on status
                boolean isPending = status == null || status.equals("pending_review");
                actionsLayout.setVisibility(isPending ? View.VISIBLE : View.GONE);

                // Set business hours
                String opening = dataSnapshot.child("hours/opening").getValue(String.class);
                String closing = dataSnapshot.child("hours/closing").getValue(String.class);
                openingHours.setText(opening != null ? opening : "Not set");
                closingHours.setText(closing != null ? closing : "Not set");

                // Get document URLs
                licenseUrl = dataSnapshot.child("documents/files/owner_id/url").getValue(String.class);
                idUrl = dataSnapshot.child("documents/files/restaurant_proof/url").getValue(String.class);

                // Enable/disable document buttons based on URL availability
                viewLicenseButton.setEnabled(licenseUrl != null);
                viewIdButton.setEnabled(idUrl != null);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                finish();
            }
        });
    }

    private void updateStatus(String status) {
        databaseRef.child("documents/status").setValue(status)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Restaurant " + status, Toast.LENGTH_SHORT).show();
                documentStatus.setText(status);
                documentStatus.setTextColor(getResources().getColor(
                    status.equals("approved") ? R.color.green : R.color.orange));
                actionsLayout.setVisibility(View.GONE);
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show());
    }
} 