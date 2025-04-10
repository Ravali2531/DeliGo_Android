package com.example.deligoandroid.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.deligoandroid.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DriverDetailsActivity extends AppCompatActivity {
    private String driverId;
    private DatabaseReference driverRef;
    
    // UI Elements
    private TextView nameText;
    private TextView emailText;
    private TextView phoneText;
    private TextView statusText;
    private View viewLicenseButton;
    private View viewGovtIdButton;
    private View actionsLayout;
    private Button approveButton;
    private Button rejectButton;
    private TextView doneButton;

    // Document URLs
    private String licenseUrl;
    private String govtIdUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_details);

        // Get driver ID from intent
        driverId = getIntent().getStringExtra("driver_id");
        if (driverId == null) {
            Toast.makeText(this, "Error: Driver ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase reference
        driverRef = FirebaseDatabase.getInstance().getReference().child("drivers").child(driverId);

        // Initialize views
        initializeViews();
        
        // Load driver data
        loadDriverDetails();

        // Setup click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        nameText = findViewById(R.id.driverName);
        emailText = findViewById(R.id.driverEmail);
        phoneText = findViewById(R.id.driverPhone);
        statusText = findViewById(R.id.documentStatus);
        viewLicenseButton = findViewById(R.id.viewLicenseButton);
        viewGovtIdButton = findViewById(R.id.viewVehicleButton);
        actionsLayout = findViewById(R.id.actionsLayout);
        approveButton = findViewById(R.id.approveButton);
        rejectButton = findViewById(R.id.rejectButton);
        doneButton = findViewById(R.id.doneButton);
    }

    private void loadDriverDetails() {
        driverRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(DriverDetailsActivity.this, "Driver not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Get basic info
                String fullName = snapshot.child("fullName").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);
                String phone = snapshot.child("phone").getValue(String.class);

                // Set text views
                nameText.setText(fullName);
                emailText.setText(email);
                phoneText.setText(phone);

                // Get documents data
                DataSnapshot documentsSnapshot = snapshot.child("documents");
                
                // Get document URLs if they exist
                DataSnapshot licenseSnapshot = documentsSnapshot.child("license");
                if (licenseSnapshot.exists()) {
                    licenseUrl = licenseSnapshot.child("url").getValue(String.class);
                }

                DataSnapshot govtIdSnapshot = documentsSnapshot.child("govt_id");
                if (govtIdSnapshot.exists()) {
                    govtIdUrl = govtIdSnapshot.child("url").getValue(String.class);
                }

                // Get status
                String status = documentsSnapshot.child("status").getValue(String.class);
                updateStatus(status);

                // Show/hide action buttons based on status
                boolean shouldShowActions = status == null || status.isEmpty() || 
                    (!status.equalsIgnoreCase("approved") && !status.equalsIgnoreCase("rejected"));
                actionsLayout.setVisibility(shouldShowActions ? View.VISIBLE : View.GONE);

                // Enable/disable document buttons based on URL availability
                boolean hasLicense = licenseUrl != null && !licenseUrl.isEmpty();
                boolean hasGovtId = govtIdUrl != null && !govtIdUrl.isEmpty();

                viewLicenseButton.setEnabled(hasLicense);
                viewGovtIdButton.setEnabled(hasGovtId);

                if (!hasLicense || !hasGovtId) {
                    Toast.makeText(DriverDetailsActivity.this, 
                        "Some documents are not available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DriverDetailsActivity.this, 
                    "Error loading driver details: " + error.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStatus(String status) {
        if (status == null || status.isEmpty()) {
            statusText.setText("Pending Review");
            statusText.setTextColor(getResources().getColor(R.color.orange));
            return;
        }
        
        switch (status.toLowerCase()) {
            case "approved":
                statusText.setText("Approved");
                statusText.setTextColor(getResources().getColor(R.color.green));
                break;
            case "rejected":
                statusText.setText("Rejected");
                statusText.setTextColor(getResources().getColor(R.color.error_light));
                break;
            default:
                statusText.setText("Pending Review");
                statusText.setTextColor(getResources().getColor(R.color.orange));
                break;
        }
    }

    private void setupClickListeners() {
        approveButton.setOnClickListener(v -> approveDriver());
        rejectButton.setOnClickListener(v -> rejectDriver());
        doneButton.setOnClickListener(v -> finish());

        viewLicenseButton.setOnClickListener(v -> {
            if (licenseUrl != null && !licenseUrl.isEmpty()) {
                viewDocument("license", licenseUrl);
            } else {
                Toast.makeText(this, "License document not available", Toast.LENGTH_SHORT).show();
            }
        });

        viewGovtIdButton.setOnClickListener(v -> {
            if (govtIdUrl != null && !govtIdUrl.isEmpty()) {
                viewDocument("govt_id", govtIdUrl);
            } else {
                Toast.makeText(this, "Government ID not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void approveDriver() {
        updateDriverStatus("approved");
    }

    private void rejectDriver() {
        updateDriverStatus("rejected");
    }

    private void updateDriverStatus(String status) {
        driverRef.child("documents").child("status").setValue(status)
            .addOnSuccessListener(aVoid -> {
                String message = status.equals("approved") ? 
                    "Driver approved successfully" : "Driver rejected";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                
                // Update timestamp
                driverRef.child("updatedAt").setValue(System.currentTimeMillis());
                
                // Send notification to driver
                sendNotificationToDriver(status);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, 
                    "Failed to update status: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
    }

    private void sendNotificationToDriver(String status) {
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
            .getReference()
            .child("notifications")
            .child(driverId)
            .push();

        String message = status.equals("approved") ? 
            "Your driver account has been approved! You can now start accepting orders." :
            "Your driver account application has been rejected. Please contact support for more information.";

        notificationsRef.child("message").setValue(message);
        notificationsRef.child("timestamp").setValue(System.currentTimeMillis());
        notificationsRef.child("type").setValue("account_" + status);
        notificationsRef.child("read").setValue(false);
    }

    private void viewDocument(String documentType, String documentUrl) {
        Intent intent = new Intent(this, DocumentPreviewActivity.class);
//        intent.putExtra("user_id", driverId);
//        intent.putExtra("document_type", documentType);
//        intent.putExtra("user_type", "drivers");
//        intent.putExtra("document_url", documentUrl);
        intent.putExtra(DocumentPreviewActivity.EXTRA_IMAGE_URL, documentUrl);
        startActivity(intent);
    }
} 