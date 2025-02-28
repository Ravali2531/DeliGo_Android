package com.example.deligoandroid.Driver;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deligoandroid.MainActivity;
import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class DriverDocumentsActivity extends AppCompatActivity {

    private ImageView govtIdPreview, licensePreview;
    private Button uploadGovtIdButton, uploadLicenseButton, submitButton;
    private Uri govtIdUri, licenseUri;
    private FirebaseStorage storage;
    private DatabaseReference databaseRef;
    private String userId;

    private final ActivityResultLauncher<String> govtIdPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    govtIdUri = uri;
                    govtIdPreview.setImageURI(uri);
                    updateSubmitButtonState();
                }
            });

    private final ActivityResultLauncher<String> licensePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    licenseUri = uri;
                    licensePreview.setImageURI(uri);
                    updateSubmitButtonState();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_documents);

        // Initialize Firebase
        storage = FirebaseStorage.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize views
        initializeViews();

        // Setup click listeners
        uploadGovtIdButton.setOnClickListener(v -> govtIdPicker.launch("image/*"));
        uploadLicenseButton.setOnClickListener(v -> licensePicker.launch("image/*"));
        submitButton.setOnClickListener(v -> uploadDocuments());
    }

    private void initializeViews() {
        govtIdPreview = findViewById(R.id.govtIdPreview);
        licensePreview = findViewById(R.id.licensePreview);
        uploadGovtIdButton = findViewById(R.id.uploadGovtIdButton);
        uploadLicenseButton = findViewById(R.id.uploadLicenseButton);
        submitButton = findViewById(R.id.submitButton);
    }

    private void updateSubmitButtonState() {
        submitButton.setEnabled(govtIdUri != null && licenseUri != null);
    }

    private void uploadDocuments() {
        submitButton.setEnabled(false);
        submitButton.setText("Uploading...");

        // Upload government ID
        uploadFile(govtIdUri, "govt_id", () -> {
            // After govt ID upload, upload license
            uploadFile(licenseUri, "license", () -> {
                // After both uploads complete
                updateDriverStatus();
            });
        });
    }

    private void uploadFile(Uri fileUri, String type, Runnable onComplete) {
        StorageReference ref = storage.getReference()
                .child("driver_documents")
                .child(userId)
                .child(type + "_" + System.currentTimeMillis());

        ref.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Save download URL to database
                        databaseRef.child("drivers")
                                .child(userId)
                                .child("documents")
                                .child(type)
                                .setValue(uri.toString())
                                .addOnSuccessListener(aVoid -> onComplete.run())
                                .addOnFailureListener(e -> handleError(e.getMessage()));
                    });
                })
                .addOnFailureListener(e -> handleError(e.getMessage()));
    }

    private void updateDriverStatus() {
        databaseRef.child("drivers")
                .child(userId)
                .child("documents")
                .child("status")
                .setValue("pending_review")  // Can be: pending_review, approved, rejected
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Documents uploaded successfully! They will be reviewed shortly.", 
                                 Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, DriverHomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> handleError(e.getMessage()));
    }

    private void handleError(String error) {
        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
        submitButton.setEnabled(true);
        submitButton.setText("Submit Documents");
    }
} 