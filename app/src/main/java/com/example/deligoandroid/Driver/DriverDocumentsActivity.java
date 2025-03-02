package com.example.deligoandroid.Driver;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

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
                    updatePreview(govtIdPreview, uri);
                    updateSubmitButtonState();
                }
            });

    private final ActivityResultLauncher<String> licensePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    licenseUri = uri;
                    updatePreview(licensePreview, uri);
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
        uploadGovtIdButton.setOnClickListener(v -> openDocumentPicker(govtIdPicker));
        uploadLicenseButton.setOnClickListener(v -> openDocumentPicker(licensePicker));
        submitButton.setOnClickListener(v -> uploadDocuments());

        // Create initial database structure
        createInitialStructure();
    }

    private void createInitialStructure() {
        // First check if structure already exists
        databaseRef.child("drivers").child(userId).get()
            .addOnSuccessListener(dataSnapshot -> {
                if (!dataSnapshot.exists()) {
                    Map<String, Object> initialData = new HashMap<>();
                    initialData.put("documentsSubmitted", false);
                    initialData.put("documents/status", "not_submitted");
                    initialData.put("documents/files", new HashMap<>());

                    databaseRef.child("drivers")
                            .child(userId)
                            .setValue(initialData)
                            .addOnFailureListener(e -> handleError("Failed to create initial structure: " + e.getMessage()));
                }
            })
            .addOnFailureListener(e -> handleError("Failed to check initial structure: " + e.getMessage()));
    }

    private void openDocumentPicker(ActivityResultLauncher<String> picker) {
        try {
            picker.launch("image/*");
        } catch (Exception e) {
            handleError("Failed to open image picker: " + e.getMessage());
        }
    }

    private void updatePreview(ImageView preview, Uri uri) {
        try {
            if (preview == govtIdPreview) {
                preview.setImageURI(null); // Clear the previous image
                preview.setImageURI(uri);
            } else if (preview == licensePreview) {
                preview.setImageURI(null); // Clear the previous image
                preview.setImageURI(uri);
            }
        } catch (Exception e) {
            handleError("Failed to update preview: " + e.getMessage());
            if (preview == govtIdPreview) {
                preview.setImageResource(R.drawable.id_placeholder);
            } else {
                preview.setImageResource(R.drawable.license_placeholder);
            }
        }
    }

    private void initializeViews() {
        govtIdPreview = findViewById(R.id.govtIdPreview);
        licensePreview = findViewById(R.id.licensePreview);
        uploadGovtIdButton = findViewById(R.id.uploadGovtIdButton);
        uploadLicenseButton = findViewById(R.id.uploadLicenseButton);
        submitButton = findViewById(R.id.submitButton);

        // Set initial placeholder images
        govtIdPreview.setImageResource(R.drawable.id_placeholder);
        licensePreview.setImageResource(R.drawable.license_placeholder);
    }

    private void updateSubmitButtonState() {
        submitButton.setEnabled(govtIdUri != null && licenseUri != null);
    }

    private void uploadDocuments() {
        if (govtIdUri == null || licenseUri == null) {
            Toast.makeText(this, "Please select both documents", Toast.LENGTH_SHORT).show();
            return;
        }

        submitButton.setEnabled(false);
        submitButton.setText("Uploading...");

        try {
            // Upload government ID first
            uploadFile(govtIdUri, "govt_id", () -> {
                // Then upload license
                uploadFile(licenseUri, "license", this::finalizeUpload);
            });
        } catch (Exception e) {
            handleError("Failed to start upload: " + e.getMessage());
        }
    }

    private void uploadFile(Uri fileUri, String type, Runnable onComplete) {
        if (fileUri == null) {
            handleError("Image URI is null for " + type);
            return;
        }

        StorageReference ref = storage.getReference()
                .child("driver_documents")
                .child(userId)
                .child(type + "_" + System.currentTimeMillis() + ".jpg");

        // Create a map for the file metadata
        Map<String, Object> fileData = new HashMap<>();
        fileData.put("uploadTime", System.currentTimeMillis());

        ref.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    ref.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                fileData.put("url", uri.toString());
                                
                                // Update the database with file information
                                databaseRef.child("drivers")
                                        .child(userId)
                                        .child("documents")
                                        .child("files")
                                        .child(type)
                                        .setValue(fileData)
                                        .addOnSuccessListener(aVoid -> onComplete.run())
                                        .addOnFailureListener(e -> handleError("Failed to save file data: " + e.getMessage()));
                            })
                            .addOnFailureListener(e -> handleError("Failed to get download URL: " + e.getMessage()));
                })
                .addOnFailureListener(e -> handleError("Failed to upload file: " + e.getMessage()));
    }

    private void finalizeUpload() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("documentsSubmitted", true);
        updates.put("documents/status", "pending_review");

        databaseRef.child("drivers")
                .child(userId)
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, 
                        "Documents uploaded successfully! They will be reviewed shortly.", 
                        Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, DriverHomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> handleError("Failed to finalize upload: " + e.getMessage()));
    }

    private void handleError(String error) {
        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
        submitButton.setEnabled(true);
        submitButton.setText("Submit Documents");
    }
} 