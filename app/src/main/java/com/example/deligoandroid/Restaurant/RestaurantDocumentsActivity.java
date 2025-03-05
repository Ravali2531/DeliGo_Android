package com.example.deligoandroid.Restaurant;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TimePicker;
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

public class RestaurantDocumentsActivity extends AppCompatActivity {

    private ImageView restaurantProofPreview, ownerIdPreview;
    private Button uploadRestaurantProofButton, uploadOwnerIdButton, submitButton;
    private TimePicker openingTimePicker, closingTimePicker;
    private Uri restaurantProofUri, ownerIdUri;
    private FirebaseStorage storage;
    private DatabaseReference databaseRef;
    private String userId;

    private final ActivityResultLauncher<String> restaurantProofPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    restaurantProofUri = uri;
                    updatePreview(restaurantProofPreview, uri);
                    updateSubmitButtonState();
                }
            });

    private final ActivityResultLauncher<String> ownerIdPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    ownerIdUri = uri;
                    updatePreview(ownerIdPreview, uri);
                    updateSubmitButtonState();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_documents);

        // Initialize Firebase
        storage = FirebaseStorage.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize views
        initializeViews();

        // Setup click listeners
        uploadRestaurantProofButton.setOnClickListener(v -> openDocumentPicker(restaurantProofPicker));
        uploadOwnerIdButton.setOnClickListener(v -> openDocumentPicker(ownerIdPicker));
        submitButton.setOnClickListener(v -> uploadDocuments());

        // Create initial database structure
        createInitialStructure();
    }

    private void initializeViews() {
        restaurantProofPreview = findViewById(R.id.restaurantProofPreview);
        ownerIdPreview = findViewById(R.id.ownerIdPreview);
        uploadRestaurantProofButton = findViewById(R.id.uploadRestaurantProofButton);
        uploadOwnerIdButton = findViewById(R.id.uploadOwnerIdButton);
        submitButton = findViewById(R.id.submitButton);
        openingTimePicker = findViewById(R.id.openingTimePicker);
        closingTimePicker = findViewById(R.id.closingTimePicker);

        // Set 24-hour format for time pickers
        openingTimePicker.setIs24HourView(true);
        closingTimePicker.setIs24HourView(true);

        // Set initial placeholder images
        restaurantProofPreview.setImageResource(R.drawable.id_placeholder);
        ownerIdPreview.setImageResource(R.drawable.id_placeholder);
    }

    private void createInitialStructure() {
        databaseRef.child("restaurants").child(userId).get()
            .addOnSuccessListener(dataSnapshot -> {
                if (!dataSnapshot.exists()) {
                    Map<String, Object> initialData = new HashMap<>();
                    initialData.put("documentsSubmitted", false);
                    initialData.put("documents/status", "not_submitted");
                    initialData.put("documents/files", new HashMap<>());

                    databaseRef.child("restaurants")
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
            preview.setImageURI(null);
            preview.setImageURI(uri);
        } catch (Exception e) {
            handleError("Failed to update preview: " + e.getMessage());
            preview.setImageResource(R.drawable.id_placeholder);
        }
    }

    private void updateSubmitButtonState() {
        submitButton.setEnabled(restaurantProofUri != null && ownerIdUri != null);
    }

    private void uploadDocuments() {
        if (restaurantProofUri == null || ownerIdUri == null) {
            Toast.makeText(this, "Please select both documents", Toast.LENGTH_SHORT).show();
            return;
        }

        submitButton.setEnabled(false);
        submitButton.setText("Uploading...");

        // Save opening and closing hours
        Map<String, String> hours = new HashMap<>();
        hours.put("opening", String.format("%02d:%02d", openingTimePicker.getHour(), openingTimePicker.getMinute()));
        hours.put("closing", String.format("%02d:%02d", closingTimePicker.getHour(), closingTimePicker.getMinute()));

        databaseRef.child("restaurants").child(userId).child("hours").setValue(hours)
            .addOnSuccessListener(aVoid -> {
                // After saving hours, upload documents
                uploadFile(restaurantProofUri, "restaurant_proof", () -> {
                    uploadFile(ownerIdUri, "owner_id", this::finalizeUpload);
                });
            })
            .addOnFailureListener(e -> handleError("Failed to save business hours: " + e.getMessage()));
    }

    private void uploadFile(Uri fileUri, String type, Runnable onComplete) {
        if (fileUri == null) {
            handleError("Image URI is null for " + type);
            return;
        }

        StorageReference ref = storage.getReference()
                .child("restaurant_documents")
                .child(userId)
                .child(type + "_" + System.currentTimeMillis() + ".jpg");

        Map<String, Object> fileData = new HashMap<>();
        fileData.put("uploadTime", System.currentTimeMillis());

        ref.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    ref.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                fileData.put("url", uri.toString());
                                
                                databaseRef.child("restaurants")
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

        databaseRef.child("restaurants")
                .child(userId)
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, 
                        "Documents uploaded successfully! They will be reviewed shortly.", 
                        Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, RestaurantHomeActivity.class));
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