package com.example.deligoandroid.Restaurant;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.model.LocationBias;
import com.google.android.libraries.places.api.model.LocationRestriction;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

public class RestaurantDocumentsActivity extends AppCompatActivity {

    private ImageView restaurantProofPreview, ownerIdPreview;
    private Button uploadRestaurantProofButton, uploadOwnerIdButton, submitButton;
    private TimePicker openingTimePicker, closingTimePicker;
    private TextView coordinatesText;
    private Uri restaurantProofUri, ownerIdUri;
    private FirebaseStorage storage;
    private DatabaseReference databaseRef;
    private String userId;
    private double selectedLatitude = 0;
    private double selectedLongitude = 0;
    private String selectedAddress = "";

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

        // Initialize Places API with custom options
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }

        // Initialize Firebase
        storage = FirebaseStorage.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize views
        initializeViews();

        // Setup Places Autocomplete
        setupPlacesAutocomplete();

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
        coordinatesText = findViewById(R.id.coordinatesText);

        // Set 24-hour format for time pickers
        openingTimePicker.setIs24HourView(true);
        closingTimePicker.setIs24HourView(true);

        // Set initial placeholder images
        restaurantProofPreview.setImageResource(R.drawable.id_placeholder);
        ownerIdPreview.setImageResource(R.drawable.id_placeholder);
    }

    private void setupPlacesAutocomplete() {
        // Initialize Places Fragment
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        if (autocompleteFragment != null) {
            // Specify the types of place data to return
            autocompleteFragment.setPlaceFields(Arrays.asList(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.ADDRESS,
                    Place.Field.LAT_LNG,
                    Place.Field.ADDRESS_COMPONENTS
            ));

            // Set type filter to establishments and addresses
            autocompleteFragment.setTypeFilter(TypeFilter.ADDRESS);
            
            // Set hint text
            autocompleteFragment.setHint("Enter restaurant address");

            // Set location bias to Canada
            // This creates a bounding box that covers most of Canada
            LatLngBounds canadaBounds = new LatLngBounds(
                new LatLng(41.676556, -141.001875),  // SW corner
                new LatLng(83.110626, -52.619062)    // NE corner
            );
            
            autocompleteFragment.setLocationBias(RectangularBounds.newInstance(canadaBounds));

            // Set country restriction to Canada
            autocompleteFragment.setCountries("CA");

            // Set up a PlaceSelectionListener to handle the response
            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    if (place.getLatLng() != null) {
                        selectedLatitude = place.getLatLng().latitude;
                        selectedLongitude = place.getLatLng().longitude;
                        selectedAddress = place.getAddress();
                        
                        // Update UI
                        coordinatesText.setText(String.format("%.6f, %.6f", selectedLatitude, selectedLongitude));
                        updateSubmitButtonState();
                    }
                }

                @Override
                public void onError(Status status) {
                    Toast.makeText(RestaurantDocumentsActivity.this,
                            "Error: " + status.getStatusMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
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
        submitButton.setEnabled(restaurantProofUri != null && 
                              ownerIdUri != null && 
                              selectedLatitude != 0 && 
                              selectedLongitude != 0);
    }

    private void uploadDocuments() {
        if (restaurantProofUri == null || ownerIdUri == null) {
            Toast.makeText(this, "Please select both documents", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedLatitude == 0 || selectedLongitude == 0) {
            Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show();
            return;
        }

        submitButton.setEnabled(false);
        submitButton.setText("Uploading...");

        // Save opening and closing hours
        Map<String, Object> restaurantData = new HashMap<>();
        restaurantData.put("hours/opening", String.format("%02d:%02d", openingTimePicker.getHour(), openingTimePicker.getMinute()));
        restaurantData.put("hours/closing", String.format("%02d:%02d", closingTimePicker.getHour(), closingTimePicker.getMinute()));
        
        // Save location data
        restaurantData.put("location/latitude", selectedLatitude);
        restaurantData.put("location/longitude", selectedLongitude);
        restaurantData.put("location/address", selectedAddress);
        restaurantData.put("store_info/address", selectedAddress);

        databaseRef.child("restaurants").child(userId).updateChildren(restaurantData)
            .addOnSuccessListener(aVoid -> {
                // After saving basic info, upload documents
                uploadFile(restaurantProofUri, "restaurant_proof", () -> {
                    uploadFile(ownerIdUri, "owner_id", this::finalizeUpload);
                });
            })
            .addOnFailureListener(e -> handleError("Failed to save restaurant data: " + e.getMessage()));
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